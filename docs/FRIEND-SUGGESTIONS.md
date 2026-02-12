# Friend Suggestions Algorithm

## Overview

The friend suggestions system recommends users to follow using a **3-tier ranking strategy** that prioritizes social relevance over raw popularity. It mirrors how Instagram and Facebook surface "People you may know."

## Tier Breakdown

### Tier 1 — Graph-Based (2nd-Degree Connections)

The primary signal. Finds users who are followed by people you already follow (friends of friends), then ranks them using a composite score.

**How it works:**

1. A single native SQL query (`findGraphBasedSuggestions`) joins `users` with `user_follows` to find 2nd-degree connections, computing `mutual_connection_count` and `follower_count` inline via `COUNT + GROUP BY`.
2. Results are scored in Java using a composite formula:

```
score = (mutualConnectionCount * 10)
      + (sameCountry ? 5 : 0)
      + min(log2(followerCount + 1), 10)
```

| Signal | Weight | Rationale |
|--------|--------|-----------|
| Mutual connections | ×10 per mutual | Strongest relevance indicator — shared social graph |
| Same country | +5 flat | Geographic proximity suggests real-world connection |
| Follower count | 0–10 (log scale) | Mild popularity boost, capped to prevent celebrities from dominating |

3. Top results are selected after scoring.
4. A **batch query** (`findMutualConnectionUsernames`) fetches the actual usernames of mutual connections for all suggestions in one roundtrip, enabling rich reason strings.

**Reason format examples:**
- `"Followed by john_doe"` — 1 mutual
- `"Followed by john_doe and jane_doe"` — 2 mutuals
- `"Followed by john_doe, jane_doe, and 3 others"` — 4+ mutuals

### Tier 2 — Same Country (Geographic Fallback)

Activates when Tier 1 doesn't fill all slots and the user has a `country_code` set.

- Queries users from the same country who aren't already followed and weren't in Tier 1 results.
- Ordered by follower count (popularity within the country).
- Reason: `"From your country"`

### Tier 3 — Popular Users (Cold Start)

Fallback for new users who don't follow anyone yet, or when Tiers 1–2 still leave empty slots.

- Returns the most-followed active users not already followed.
- Reason: `"Popular on Luna"`

## Query Strategy

The previous implementation had an **N+1 problem** — for each of N suggestions, it fired 2 extra queries (mutual count + follower count), resulting in 20+ DB roundtrips for 10 suggestions.

The current implementation uses **3 queries maximum** for a full response:

| # | Query | Purpose |
|---|-------|---------|
| 1 | `findGraphBasedSuggestions` | Fetch 2nd-degree connections with mutual count + follower count computed inline |
| 2 | `findMutualConnectionUsernames` | Batch-fetch mutual usernames for all Tier 1 results |
| 3 | `findSameCountryUsersExcluding` or `findPopularUsersExcluding` | Fill remaining slots if needed |

## Data Flow

```
GET /api/users/suggestions?limit=10
        │
        ▼
┌─────────────────────────┐
│  Load current user      │  ← fetch country_code
│  Count following         │  ← determines if Tier 1 applies
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────┐
│  TIER 1: Graph query    │  ← single query, returns projections
│  Composite scoring      │  ← scored + sorted in Java
│  Batch mutual usernames │  ← single query for all results
└─────────┬───────────────┘
          │ slots remaining?
          ▼
┌─────────────────────────┐
│  TIER 2: Same country   │  ← only if country_code is set
└─────────┬───────────────┘
          │ slots remaining?
          ▼
┌─────────────────────────┐
│  TIER 3: Popular users  │  ← cold start fallback
└─────────┬───────────────┘
          │
          ▼
    Return combined list
```

## Response Shape

```json
{
  "id": 42,
  "username": "alice",
  "profileImageUrl": "https://res.cloudinary.com/...",
  "followerCount": 1250,
  "mutualConnections": 4,
  "mutualConnectionUsernames": ["john_doe", "jane_doe"],
  "suggestionReason": "Followed by john_doe, jane_doe, and 2 others",
  "isFollowing": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `mutualConnections` | int | Total number of mutual connections |
| `mutualConnectionUsernames` | string[] | Usernames of mutual connections (for client-side display) |
| `suggestionReason` | string | Human-readable reason string |

## Key Files

| File | Role |
|------|------|
| `user/dto/UserSuggestionProjection.java` | Projection interface for graph query results |
| `user/dto/UserSuggestionResponse.java` | API response DTO |
| `user/repository/UserRepository.java` | `findGraphBasedSuggestions`, `findSameCountryUsersExcluding` |
| `user/repository/UserFollowRepository.java` | `findMutualConnectionUsernames` batch query |
| `user/service/impl/UserServiceImpl.java` | Orchestrates the 3-tier logic and scoring |
