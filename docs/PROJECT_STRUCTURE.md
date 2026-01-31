# Luna - Project Structure

## Feature-Based Architecture

This project follows a **feature-based structure** where code is organized by business domain rather than technical layers.

```
src/main/java/com/luna/
├── LunaApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── CloudinaryConfig.java
│   ├── OpenApiConfig.java
│   └── WebConfig.java
│
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   └── ServiceApiKeyFilter.java
│
├── common/
│   ├── controller/
│   │   └── HealthController.java
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── ErrorDetails.java
│   │   ├── PaginationMeta.java
│   │   └── ResponseMeta.java
│   ├── exception/
│   │   ├── BadRequestException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── UnauthorizedException.java
│   └── service/
│       ├── CloudinaryService.java
│       ├── EmailService.java
│       ├── GeoIpService.java
│       └── RateLimitService.java
│
├── auth/
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── IAuthService.java
│   │   └── impl/AuthServiceImpl.java
│   └── dto/
│       ├── AuthRequest.java
│       ├── AuthResponse.java
│       ├── LoginResponse.java
│       ├── RegisterRequest.java
│       ├── RefreshTokenRequest.java
│       ├── ForgotPasswordRequest.java
│       ├── ResetPasswordRequest.java
│       ├── VerifyEmailRequest.java
│       ├── VerifyDeviceRequest.java
│       └── ResendOtpRequest.java
│
├── user/
│   ├── controller/
│   │   ├── UserController.java
│   │   ├── FollowController.java
│   │   └── InternalUserController.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── RefreshToken.java
│   │   ├── UserDevice.java
│   │   ├── UserFollow.java
│   │   ├── VerificationToken.java
│   │   ├── DeviceVerificationToken.java
│   │   └── PasswordResetToken.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   ├── UserDeviceRepository.java
│   │   ├── UserFollowRepository.java
│   │   ├── VerificationTokenRepository.java
│   │   ├── DeviceVerificationTokenRepository.java
│   │   └── PasswordResetTokenRepository.java
│   ├── service/
│   │   ├── IUserService.java
│   │   ├── IFollowService.java
│   │   └── impl/
│   │       ├── UserServiceImpl.java
│   │       └── FollowServiceImpl.java
│   └── dto/
│       ├── UserProfileResponse.java
│       ├── UserSuggestionResponse.java
│       └── UpdateBioRequest.java
│
├── post/
│   ├── controller/
│   │   ├── PostController.java
│   │   └── HashtagController.java
│   ├── entity/
│   │   ├── Post.java
│   │   ├── PostLike.java
│   │   ├── SavedPost.java
│   │   ├── Repost.java
│   │   ├── Hashtag.java
│   │   └── PostHashtag.java
│   ├── repository/
│   │   ├── PostRepository.java
│   │   ├── PostLikeRepository.java
│   │   ├── SavedPostRepository.java
│   │   ├── RepostRepository.java
│   │   ├── HashtagRepository.java
│   │   └── PostHashtagRepository.java
│   ├── scheduler/
│   │   └── PostCleanupScheduler.java
│   ├── service/
│   │   ├── IPostService.java
│   │   ├── HashtagService.java
│   │   └── impl/PostServiceImpl.java
│   └── dto/
│       ├── CreatePostRequest.java
│       ├── PostResponse.java
│       ├── RepostRequest.java
│       ├── RepostResponse.java
│       └── HashtagResponse.java
│
├── comment/
│   ├── controller/
│   │   └── CommentController.java
│   ├── entity/
│   │   └── Comment.java
│   ├── repository/
│   │   └── CommentRepository.java
│   ├── service/
│   │   ├── ICommentService.java
│   │   └── impl/CommentServiceImpl.java
│   └── dto/
│       ├── CreateCommentRequest.java
│       └── CommentResponse.java
│
└── activity/
    ├── controller/
    │   └── ActivityController.java
    ├── entity/
    │   ├── Activity.java
    │   └── ActivityType.java
    ├── repository/
    │   └── ActivityRepository.java
    ├── service/
    │   ├── IActivityService.java
    │   └── impl/ActivityServiceImpl.java
    └── dto/
        └── ActivityResponse.java
```

## Features

### Auth
- User registration with email verification (OTP)
- Login with JWT tokens (access + refresh)
- Device verification for new devices
- Forgot password / Reset password
- Rate limiting on auth endpoints
- Country detection from IP (GeoIP)

### User
- Profile management (image, bio)
- User search by username
- Follow suggestions (friends of friends, popular users)
- Follow/unfollow users

### Post
- Create posts with images or videos (unlimited count, size limited)
- Like/unlike posts
- Save/unsave posts (bookmarks)
- Repost/share with optional quote
- Soft delete with 30-day recovery
- Hashtag extraction and indexing

### Comment
- Nested comments (3 levels max)
- Edit/delete own comments

### Hashtag
- Auto-extracted from post content
- Trending hashtags (last 24h)
- Search hashtags
- Browse posts by hashtag

### Activity
- Activity logging for user actions

## API Endpoints

### Auth (`/api/auth`)
- `POST /register` - Register new user
- `POST /verify-email` - Verify email with OTP
- `POST /resend-otp` - Resend verification OTP
- `POST /login` - Login (may require device verification)
- `POST /verify-device` - Verify new device
- `POST /resend-device-otp` - Resend device OTP
- `POST /refresh` - Refresh access token
- `POST /forgot-password` - Request password reset
- `POST /reset-password` - Reset password with OTP

### Users (`/api/users`)
- `GET /profile` - Get current user profile
- `GET /{userId}/profile` - Get user profile by ID
- `PUT /profile/image` - Update profile image
- `PUT /profile/bio` - Update bio
- `GET /suggestions` - Get follow suggestions
- `GET /search?q=` - Search users

### Follow (`/api/follow`)
- `POST /{userId}` - Follow user
- `DELETE /{userId}` - Unfollow user

### Posts (`/api/posts`)
- `POST /` - Create post
- `GET /{postId}` - Get post
- `GET /user/{userId}` - Get user's posts
- `GET /timeline` - Get timeline (followed users' posts)
- `DELETE /{postId}` - Soft delete post
- `POST /{postId}/restore` - Restore deleted post
- `POST /{postId}/like` - Like post
- `DELETE /{postId}/like` - Unlike post
- `POST /{postId}/save` - Save post
- `DELETE /{postId}/save` - Unsave post
- `GET /saved` - Get saved posts
- `POST /{postId}/repost` - Repost
- `DELETE /{postId}/repost` - Undo repost
- `GET /user/{userId}/reposts` - Get user's reposts

### Comments (`/api`)
- `POST /posts/{postId}/comments` - Create comment
- `GET /posts/{postId}/comments` - Get post comments
- `GET /comments/{commentId}` - Get comment
- `PUT /comments/{commentId}` - Update comment
- `DELETE /comments/{commentId}` - Delete comment

### Hashtags (`/api/hashtags`)
- `GET /trending` - Get trending hashtags
- `GET /search?q=` - Search hashtags
- `GET /{hashtag}/posts` - Get posts by hashtag

## Environment Variables

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/luna_db
DB_USERNAME=postgres
DB_PASSWORD=

# JWT
JWT_SECRET=your-secret-key
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Mail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# Cloudinary
CLOUDINARY_URL=cloudinary://...

# Service API Key
SERVICE_API_KEY=

# Feature Flags
DEVICE_VERIFICATION_ENABLED=true
```

## Database Migrations

Located in `src/main/resources/db/migration/`:
- V1: Initial schema (users, refresh_tokens)
- V2: Email verification
- V3: Device verification
- V4: Posts and follows
- V5: Activities
- V7: Media JSON arrays
- V8: Comments
- V9: User country
- V10: Password reset tokens
- V11: User bio
- V12: Saved posts
- V13: Reposts
- V14: Hashtags
