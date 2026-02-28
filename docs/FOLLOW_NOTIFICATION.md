# Follow Notification — Implementation To-Do List

Send an FCM push notification to user B when user A follows them.
Redis prevents spam when someone rapidly toggles follow/unfollow.

---

## How the spam prevention works

Redis key: `notification:follow:cooldown:{followerId}:{followedId}` (TTL: 1 hour)

- User A follows B → notification sent → key written
- User A unfollows B → key left alone (intentional, expires on its own)
- User A follows B again within 1 hour → key still exists → notification skipped
- After 1 hour → key gone → next follow triggers a new notification

---

## To-Do

### Step 1 — Dependencies

- [x] Add `com.google.firebase:firebase-admin:9.3.0` to `build.gradle` ✓
- [x] Add `org.springframework.boot:spring-boot-starter-data-redis` to `build.gradle` ✓

---

### Step 2 — Environment & Config

- [ ] Add `REDIS_URL` to `.env.example` (e.g. `redis://localhost:6379`)
- [x] Add `FIREBASE_CREDENTIALS_JSON` to `.env` — minified (single-line) Firebase service account JSON ✓
  - Add the key to `.env.example` as an empty placeholder: `FIREBASE_CREDENTIALS_JSON=`
  - No file-based credential approach needed
- [x] Add Redis config block to `application.yml` ✓:
  ```yaml
  spring:
    data:
      redis:
        url: ${REDIS_URL:redis://localhost:6379}
        timeout: 2000ms
  ```

---

### Step 3 — Database Migration

- [ ] Create `src/main/resources/db/migration/V15__create_user_fcm_tokens.sql`:
  ```sql
  CREATE TABLE user_fcm_tokens (
      id           BIGSERIAL PRIMARY KEY,
      user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      fcm_token    TEXT         NOT NULL,
      platform     VARCHAR(20),           -- ANDROID, IOS, WEB
      device_name  VARCHAR(100),
      created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
      last_used_at TIMESTAMP    NOT NULL DEFAULT NOW(),
      CONSTRAINT uq_fcm_token UNIQUE (fcm_token)
  );
  CREATE INDEX idx_user_fcm_tokens_user_id ON user_fcm_tokens(user_id);
  ```

---

### Step 4 — Firebase Setup

- [ ] Create `src/main/java/com/luna/config/FirebaseConfig.java`
  - Read `FIREBASE_CREDENTIALS_JSON` from env, parse as `InputStream`
  - Build `FirebaseOptions` with `GoogleCredentials.fromStream(...)`
  - Initialize `FirebaseApp` (guard against double-init with `FirebaseApp.getApps()`)
  - Expose a `FirebaseMessaging` bean

---

### Step 5 — FCM Token Entity & Repository

- [ ] Create `com/luna/notification/entity/UserFcmToken.java`
  - Fields: `id`, `user` (ManyToOne → User), `fcmToken`, `platform`, `deviceName`,
    `createdAt`, `lastUsedAt`
  - `@Table(name = "user_fcm_tokens")`

- [ ] Create `com/luna/notification/repository/UserFcmTokenRepository.java`
  ```java
  List<UserFcmToken> findByUserId(Long userId);
  Optional<UserFcmToken> findByFcmToken(String fcmToken);
  void deleteByFcmToken(String fcmToken);
  ```

---

### Step 6 — DTOs

- [ ] Create `com/luna/notification/dto/RegisterFcmTokenRequest.java`
  - Fields: `fcmToken` (required), `platform` (optional), `deviceName` (optional)

- [ ] Create `com/luna/notification/dto/NotificationPayload.java` (internal record)
  - Fields: `title`, `body`, `data` (Map<String, String>)

---

### Step 7 — FCM Token Controller

- [ ] Create `com/luna/notification/controller/FcmTokenController.java`

  | Method | Path | What it does |
  |---|---|---|
  | `POST` | `/api/v1/users/me/fcm-tokens` | Save token for the authenticated user |
  | `DELETE` | `/api/v1/users/me/fcm-tokens/{token}` | Remove a specific token (call on logout) |

  - `POST`: upsert — if the token already exists, just update `lastUsedAt`; if it belongs
    to a different user, re-assign it (the old device logged out)
  - Both endpoints require JWT authentication

---

### Step 8 — FcmService (Firebase wrapper)

- [ ] Create `com/luna/notification/service/IFcmService.java`
  ```java
  void sendToUser(Long userId, NotificationPayload payload);
  ```

- [ ] Create `com/luna/notification/service/impl/FcmServiceImpl.java`
  - Fetch all FCM tokens for `userId` from the repository
  - If no tokens → return early (user has no registered devices)
  - Build a `MulticastMessage` with `Notification` + data map
  - Call `FirebaseMessaging.sendEachForMulticast(...)`
  - Iterate `BatchResponse` — for any token where the response error code is
    `UNREGISTERED` or `INVALID_ARGUMENT`, delete that token from the DB
    (stale token auto-cleanup)

---

### Step 9 — NotificationService (cooldown gate)

- [ ] Create `com/luna/notification/service/INotificationService.java`
  ```java
  void sendFollowNotification(Long followerId, Long followedUserId);
  ```

- [ ] Create `com/luna/notification/service/impl/NotificationServiceImpl.java`
  - Annotate `sendFollowNotification` with `@Async` (runs off the HTTP thread)
  - Build Redis key: `notification:follow:cooldown:{followerId}:{followedUserId}`
  - Use `StringRedisTemplate.hasKey(key)`:
    - `true` → return immediately (suppressed)
    - `false` → `opsForValue().set(key, "1", 1, TimeUnit.HOURS)` then continue
  - Fetch follower's `displayName` / `username` from `UserRepository`
  - Build `NotificationPayload`:
    ```
    title: "{followerName} started following you"
    body:  ""
    data:  { "type": "FOLLOW", "userId": "{followerId}" }
    ```
  - Call `fcmService.sendToUser(followedUserId, payload)`

---

### Step 10 — Wire into FollowServiceImpl

- [ ] Inject `INotificationService` into `FollowServiceImpl`
- [ ] At the end of `followUser()`, after the existing `activityService.logActivity(...)` call, add:
  ```java
  notificationService.sendFollowNotification(followerId, followingId);
  ```
- [ ] No changes to `unfollowUser()` — Redis key expires on its own

---

### Step 11 — Manual Testing

- [ ] Register an FCM token via `POST /api/v1/users/me/fcm-tokens`
- [ ] User A follows user B → confirm notification arrives on B's device
- [ ] User A unfollows B, then follows again within 1 hour → confirm **no** second notification
- [ ] Flush Redis (`FLUSHDB` locally) or wait 1 hour, then follow again → confirm notification arrives
- [ ] Revoke/expire a token in Firebase console → follow again → confirm stale token is deleted from DB

---

## Out of Scope (Future Work)

- In-app notification inbox (bell icon, storing notifications in the DB)
- Notifications for likes, comments, reposts
- Per-user notification preferences / mute settings
- APNs direct integration (FCM handles iOS via its APNs gateway already)
