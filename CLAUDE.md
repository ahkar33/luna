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

**Key Technologies:** PostgreSQL, Flyway migrations, Spring Security + JWT, Cloudinary (images), Bucket4j (rate limiting), SpringDoc OpenAPI

## Architecture

The codebase uses **feature-based vertical slicing** rather than layered architecture. Each feature contains its own controller, service, repository, entity, and DTO packages:

```
src/main/java/com/luna/
├── config/          # SecurityConfig, CorsConfig, CloudinaryConfig, etc.
├── security/        # JwtService, JwtAuthenticationFilter, ServiceApiKeyFilter
├── common/          # Shared utilities, exceptions, CloudinaryService, EmailService
├── auth/            # Authentication (register, login, OTP verification, password reset)
├── user/            # User profiles, follows, devices, refresh tokens
├── post/            # Posts, likes, saves, reposts, hashtags
├── comment/         # Nested comments (3 levels max)
└── activity/        # Activity logging
```

**Service Pattern:** Interface-based design (IUserService, IPostService) with implementations in `impl/` subfolders.

## Configuration

- **Main config:** `src/main/resources/application.yml`
- **Environment:** Copy `.env.example` to `.env` for local development
- **Migrations:** `src/main/resources/db/migration/` (V1-V14, Flyway versioned)

**Required environment variables:**
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL connection
- `JWT_SECRET` - Minimum 256 bits for production
- `MAIL_*` - SMTP configuration for OTP emails
- `CLOUDINARY_URL` - Image hosting
- `SERVICE_API_KEY` - Internal service-to-service auth

## Key Patterns

- **Exception Handling:** `GlobalExceptionHandler` provides consistent error responses via `ErrorDetails`
- **Authentication:** JWT tokens (15-min access, 7-day refresh) with optional device verification via email OTP
- **Rate Limiting:** Bucket4j on auth endpoints (3 attempts per 5 minutes)
- **Soft Deletes:** Posts use soft delete with daily cleanup of 30+ day old records (2 AM via `PostCleanupScheduler`)

## API Documentation

Swagger UI available at `/swagger-ui.html` when running locally. OpenAPI spec at `/api-docs`.
