# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

```bash
# Run the application locally
./gradlew bootRun

# Build
./gradlew clean build              # Full build with tests
./gradlew build -x test            # Build without tests

# Testing
./gradlew test                     # Run all tests (loads .env automatically)
./gradlew test --tests TestClass   # Run specific test class

# Dependencies
./gradlew dependencies             # Show dependency tree
```

## Project Overview

Luna is a Spring Boot 4.0.1 social media backend API using Java 21. It provides authentication, user management, posts with likes/saves/reposts, nested comments, hashtags, and activity tracking.

**Key Technologies:** PostgreSQL, Flyway migrations, Spring Security + JWT, Cloudinary (images/videos), Bucket4j (rate limiting), SpringDoc OpenAPI, Redis (notification cooldowns), Firebase FCM (push notifications)

## Architecture

The codebase uses **feature-based vertical slicing** rather than layered architecture. Each feature contains its own controller, service, repository, entity, and DTO packages:

```
src/main/java/com/luna/
├── config/          # SecurityConfig, CorsConfig, CloudinaryConfig, RedisConfig, etc.
├── security/        # JwtService, JwtAuthenticationFilter, ServiceApiKeyFilter, SecurityUtils
├── common/          # Shared utilities, exceptions, CloudinaryService, EmailService, PagedResponse, ApiResponse
├── auth/            # Authentication (register, login, OTP verification, password reset)
├── user/            # User profiles, follows, devices, refresh tokens
├── post/            # Posts, likes, saves, reposts, hashtags
├── comment/         # Nested comments (3 levels max: depth 0, 1, 2)
├── activity/        # Activity logging
└── notification/    # FCM push notifications
```

**Service Pattern:** Interface-based design (`IUserService`, `IPostService`) with implementations in `impl/` subfolders.

```
feature/
├── controller/      FeatureController.java
├── service/         IFeatureService.java
│   └── impl/        FeatureServiceImpl.java
├── repository/      FeatureRepository.java
├── entity/          Feature.java
└── dto/             FeatureRequest.java, FeatureResponse.java
```

## Configuration

- **Main config:** `src/main/resources/application.yml`
- **Environment:** Copy `.env.example` to `.env` for local development
- **Migrations:** `src/main/resources/db/migration/` (V1–V19, Flyway versioned)

**Required environment variables:**
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL connection
- `JWT_SECRET` — Minimum 256 bits for production
- `MAIL_*` — SMTP configuration for OTP emails
- `CLOUDINARY_URL` — Image/video hosting
- `SERVICE_API_KEY` — Internal service-to-service auth
- `JWT_ACCESS_TOKEN_EXPIRATION` — Default 900000 ms (15 min)
- `JWT_REFRESH_TOKEN_EXPIRATION` — Default 604800000 ms (7 days)

---

## Coding Patterns — Follow These Exactly

### Entity Pattern

```java
@Entity
@Table(name = "table_name")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- All PKs are `UUID` with `@GeneratedValue(strategy = GenerationType.UUID)`
- All FK relationships use `FetchType.LAZY` to avoid N+1 queries
- Timestamps use `@CreationTimestamp` / `@UpdateTimestamp`
- Enums stored as `@Enumerated(EnumType.STRING)`
- JSON arrays stored as `TEXT` column (serialized via ObjectMapper)

### Repository Pattern

```java
public interface MyEntityRepository extends JpaRepository<MyEntity, UUID> {

    // JPQL for complex queries
    @Query("SELECT m FROM MyEntity m WHERE m.user.id = :userId AND m.active = true ORDER BY m.createdAt DESC")
    Page<MyEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    // Derived query methods
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);
    long countByPostId(UUID postId);
    void deleteByUserId(UUID userId);

    // Native SQL only when JPQL is insufficient
    @Query(value = "SELECT * FROM my_entities WHERE ...", nativeQuery = true)
    List<MyEntity> findSomethingNative(@Param("id") UUID id);
}
```

- Extend `JpaRepository<Entity, UUID>` (all PKs are UUID)
- Prefer JPQL over native SQL; use native only for complex queries (CTEs, window functions)
- Use `Page<T>` + `Pageable` for paginated results
- `existsByXxx` for boolean checks (avoids loading the full entity)

### Service Interface Pattern

```java
public interface IMyFeatureService {
    MyEntityResponse create(CreateMyEntityRequest request, UUID userId);
    MyEntityResponse getById(UUID id, UUID currentUserId);
    Page<MyEntityResponse> getByUser(UUID userId, Pageable pageable);
    MyEntityResponse update(UUID id, UUID userId, UpdateRequest request);
    void delete(UUID id, UUID userId);
}
```

- Prefix interfaces with `I` (`IPostService`, `IUserService`)
- Method names: `createXxx`, `getXxx`, `updateXxx`, `deleteXxx`, `searchXxx`
- Always pass `UUID userId` / `UUID currentUserId` for ownership checks
- Use `Page<T>` + `Pageable` for list operations

### Service Implementation Pattern

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MyFeatureServiceImpl implements IMyFeatureService {

    private final MyEntityRepository myEntityRepository;
    private final UserRepository userRepository;
    private final IActivityService activityService;

    @Override
    @Transactional
    public MyEntityResponse create(CreateMyEntityRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MyEntity entity = MyEntity.builder()
                .user(user)
                .name(request.getName())
                .build();

        entity = myEntityRepository.save(entity);

        // Log activity asynchronously (never block on this)
        activityService.logActivity(userId, ActivityType.SOME_TYPE, "MY_ENTITY",
                entity.getId(), null, null);

        log.debug("Created entity {} for user {}", entity.getId(), userId);
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public MyEntityResponse getById(UUID id, UUID currentUserId) {
        MyEntity entity = myEntityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return mapToResponse(entity);
    }

    // Private DTO mapping — never expose entity outside service layer
    private MyEntityResponse mapToResponse(MyEntity entity) {
        return MyEntityResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
```

- `@Transactional` on write methods; `@Transactional(readOnly = true)` on reads
- `@Async` for fire-and-forget operations (activity logging, notifications) — never throw
- Throw `ResourceNotFoundException` for missing records, `UnauthorizedException` for ownership violations, `BadRequestException` for business logic violations
- Map entity → DTO in a private `mapToXxxResponse()` method; never return raw entities

### Controller Pattern

```java
@RestController
@RequestMapping("/api/my-feature")
@RequiredArgsConstructor
@Tag(name = "My Feature", description = "Operations for my feature")
@SecurityRequirement(name = "bearerAuth")
public class MyFeatureController {

    private final IMyFeatureService myFeatureService;

    @PostMapping
    @Operation(summary = "Create something")
    public ResponseEntity<MyEntityResponse> create(
            @Valid @RequestBody CreateMyEntityRequest request,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        MyEntityResponse response = myFeatureService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get by ID")
    public ResponseEntity<MyEntityResponse> getById(
            @PathVariable("id") UUID id,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        MyEntityResponse response = myFeatureService.getById(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get paginated list by user")
    public ResponseEntity<PagedResponse<MyEntityResponse>> getByUser(
            @PathVariable("userId") UUID userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyEntityResponse> results = myFeatureService.getByUser(userId, pageable);
        return ResponseEntity.ok(PagedResponse.of(results));
    }
}
```

- `SecurityUtils.getUserId(authentication)` returns `UUID` — use this, not `authentication.getName()`
- `@PathVariable` for resource IDs, `@RequestParam` for filters/pagination
- Wrap paginated responses with `PagedResponse.of(page)`
- HTTP status: `201 CREATED` for POST, `200 OK` for GET/PUT/DELETE
- Add `@Operation(summary = "...")` to every endpoint for Swagger docs

### DTO Pattern

```java
// Request DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMyEntityRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;
}

// Response DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyEntityResponse {
    private UUID id;
    private String name;
    private AuthorInfo author;   // nested inner class
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private UUID id;
        private String username;
        private String profileImageUrl;
    }
}
```

- Always use `@Data @Builder @NoArgsConstructor @AllArgsConstructor` (not Java records)
- All ID fields are `UUID`
- Nested objects use static inner classes (`AuthorInfo`, `RepostAuthor`)
- Use Jakarta Validation on request DTOs: `@NotBlank`, `@Size`, `@Email`, `@Pattern`

---

## Exception Handling

Three custom exceptions in `com.luna.common.exception`:

| Exception | HTTP Status | Use case |
|-----------|------------|----------|
| `ResourceNotFoundException` | 404 | Entity not found by ID |
| `UnauthorizedException` | 401 | User doesn't own the resource |
| `BadRequestException` | 400 | Business rule violation, invalid input |

All are unchecked (`extends RuntimeException`). `GlobalExceptionHandler` catches them and returns a consistent `ApiResponse<Object>` with an `ErrorDetails` body.

**Never** use `throw new RuntimeException(...)` — always use one of the three above.

---

## Authentication & Security

- **JWT subject is the user's email** (`User.getUsername()` returns `email`)
- The Spring Security `Authentication` principal is the full `User` entity
- Get current user: `SecurityUtils.getUserId(authentication)` → `UUID`
- Get full user: `SecurityUtils.getUser(authentication)` → `User`
- Access token: 15 min | Refresh token: 7 days
- Device verification: new devices may require email OTP before JWT is issued

---

## ID Types

- **All primary keys and foreign keys are `UUID`** (migrated from `Long` in V19)
- JPA: `@GeneratedValue(strategy = GenerationType.UUID)` — Hibernate generates UUID in Java
- Never use `Long` for entity IDs anywhere in the codebase

---

## Pagination

Services return `Page<ResponseDto>`, controllers wrap with `PagedResponse.of(page)`.

```java
// Service
Page<PostResponse> getUserPosts(UUID userId, UUID currentUserId, Pageable pageable);

// Controller
Pageable pageable = PageRequest.of(page, Math.min(size, 50));
Page<PostResponse> posts = postService.getUserPosts(userId, currentUserId, pageable);
return ResponseEntity.ok(PagedResponse.of(posts));
```

---

## Activity Logging

Log user actions asynchronously — never block the main flow:

```java
// entityType is uppercase string: "POST", "COMMENT", "REPOST", "USER"
activityService.logActivity(
    userId,           // who performed the action
    ActivityType.LIKE,// enum constant
    "POST",           // entity type string
    post.getId(),     // UUID of the entity acted on
    post.getAuthor().getId(),  // UUID of target user (owner of entity), or null
    null              // optional metadata JSON string
);
```

`ActivityType` enum values: `POST_CREATE`, `POST_DELETE`, `LIKE`, `UNLIKE`, `SAVE`, `UNSAVE`, `REPOST`, `UNDO_REPOST`, `COMMENT`, `FOLLOW`, `UNFOLLOW`, `PROFILE_UPDATE`, `LOGIN`, `REGISTER`

---

## File Uploads (Cloudinary)

```java
// Upload image/video — returns HTTPS URL
String imageUrl = cloudinaryService.uploadImage(file, "posts");   // folder = "posts"
String videoUrl = cloudinaryService.uploadVideo(file, "profiles");

// Delete by extracting public ID from URL
String publicId = cloudinaryService.extractPublicId(url);
cloudinaryService.deleteFile(publicId);
```

Limits: images max 10 MB (JPEG/PNG/WEBP), videos max 40 MB (MP4/WEBM/MOV).
Multiple URLs stored as JSON array in a `TEXT` column, serialized via `ObjectMapper`.

---

## Database Migrations

- Flyway versioned migrations in `src/main/resources/db/migration/`
- Current version: **V19** (UUID migration)
- New migrations must be `V20__description.sql`, `V21__description.sql`, etc.
- Never modify existing migration files (V1–V19)
- Always add explicit constraint names (e.g., `CONSTRAINT fk_my_feature_user FOREIGN KEY ...`)
- Use `CASCADE` when dropping PK constraints to handle any dependent FK constraints

---

## Key Patterns to Remember

- **Soft Deletes:** Posts have `deletedAt` timestamp; hard-deleted after 30 days by `PostCleanupScheduler` (2 AM daily)
- **Nested Comments:** Max 3 levels (depth 0, 1, 2); `Comment.MAX_DEPTH = 2`
- **Rate Limiting:** Bucket4j on auth endpoints — 3 attempts per 5 minutes per IP+email
- **Async operations:** Activity logging and FCM notifications use `@Async` and must never throw
- **Redis:** Used for notification cooldown keys — format `"notification:follow:cooldown:%s:%s"` (UUID strings)

---

## Project Memory

- Always consider backward compatibility when changing or developing or implementing or fixing APIs.
- Prefer additive changes over destructive ones.
