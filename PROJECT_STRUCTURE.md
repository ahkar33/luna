# Luna - Project Structure

## Feature-Based Architecture

This project follows a **feature-based structure** where code is organized by business domain rather than technical layers.

```
src/main/java/com/luna/
├── LunaApplication.java          # Main Spring Boot application
│
├── config/                       # Global configurations
│   └── SecurityConfig.java       # Spring Security configuration
│
├── security/                     # Security components (shared)
│   ├── JwtService.java          # JWT token generation/validation
│   └── JwtAuthenticationFilter.java  # JWT filter for requests
│
├── common/                       # Shared across features
│   ├── dto/                     # Common DTOs (future)
│   ├── exception/               # Global exception handlers (future)
│   ├── util/                    # Utility classes (future)
│   └── constant/                # Application constants (future)
│
├── auth/                        # Authentication feature
│   ├── controller/
│   │   └── AuthController.java  # /api/auth endpoints
│   ├── service/
│   │   └── AuthService.java     # Auth business logic
│   └── dto/
│       ├── AuthRequest.java     # Login request
│       ├── AuthResponse.java    # Auth response with tokens
│       ├── RegisterRequest.java # Registration request
│       └── RefreshTokenRequest.java
│
└── user/                        # User feature
    ├── entity/
    │   ├── User.java           # User entity (UserDetails)
    │   ├── RefreshToken.java   # Refresh token entity
    │   └── Role.java           # User roles enum
    ├── repository/
    │   ├── UserRepository.java
    │   └── RefreshTokenRepository.java
    └── service/
        └── UserService.java    # UserDetailsService implementation
```

## Benefits of Feature-Based Structure

1. **Better Scalability**: Each feature is self-contained
2. **Easier Navigation**: Related code is grouped together
3. **Team Collaboration**: Different teams can work on different features
4. **Clear Boundaries**: Feature boundaries are explicit
5. **Easier Testing**: Test files mirror the feature structure

## Current Features

### ✅ Auth Feature
- User registration
- User login
- JWT access token (15 min)
- Refresh token (7 days)
- Token refresh endpoint

### ✅ User Feature
- User entity with Spring Security integration
- User repository
- UserDetailsService implementation
- Refresh token management

## Future Features (Planned)

- **post/** - Post creation, editing, deletion
- **comment/** - Comments on posts
- **follow/** - Follow/unfollow users
- **notification/** - User notifications
- **messaging/** - Direct messages
- **media/** - Image/video upload

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/refresh` - Refresh access token

## Environment Variables

See `.env.example` for required configuration:
- Database connection (PostgreSQL)
- JWT secret and expiration times
- JPA settings
