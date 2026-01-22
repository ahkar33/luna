# Luna API Documentation

## Overview
Luna is a modern social media platform backend built with Spring Boot, featuring secure authentication, email verification, and device tracking.

## Features

### Authentication & Security
- JWT-based authentication (access + refresh tokens)
- Email verification with OTP (6-digit code)
- Device verification for new logins
- Rate limiting to prevent brute force attacks
- Password encryption with BCrypt

### Tech Stack
- **Framework:** Spring Boot 4.0.1
- **Language:** Java 21
- **Database:** PostgreSQL (Supabase)
- **Migration:** Flyway
- **Security:** Spring Security + JWT
- **Email:** JavaMailSender
- **Rate Limiting:** Bucket4j

## Documentation

- [Email Verification](./EMAIL_VERIFICATION.md) - OTP-based email verification flow
- [Device Verification](./DEVICE_VERIFICATION.md) - New device detection and security

## Quick Start

### Prerequisites
- Java 21
- PostgreSQL database
- SMTP email server (Gmail, SendGrid, etc.)

### Setup

1. **Clone and configure:**
```bash
cp .env.example .env
# Edit .env with your credentials
```

2. **Run locally:**
```bash
./gradlew bootRun
```

3. **Build for production:**
```bash
./gradlew clean build
```

### Environment Variables

```env
# Database
DB_URL=jdbc:postgresql://host:5432/dbname
DB_USERNAME=your-username
DB_PASSWORD=your-password

# JWT
JWT_SECRET=your-secret-key-min-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

## API Endpoints

### Public Endpoints (No Auth Required)

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "password123"
}
```

#### Verify Email
```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json
X-Device-Fingerprint: device-id-123

{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Verify Device (if new device detected)
```http
POST /api/auth/verify-device
Content-Type: application/json

{
  "email": "user@example.com",
  "deviceFingerprint": "device-id-123",
  "otp": "123456"
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

#### Health Check
```http
GET /health
```

### Protected Endpoints (Require JWT)

All other endpoints require the `Authorization` header:
```http
Authorization: Bearer <your-access-token>
```

## Deployment

### Docker Deployment

1. **Build image:**
```bash
docker-compose build
```

2. **Run container:**
```bash
docker-compose up -d
```

3. **Check logs:**
```bash
docker-compose logs -f
```

### EC2 Deployment

See [deploy.sh](../deploy.sh) for automated deployment script.

## Database Schema

### Tables
- `users` - User accounts
- `refresh_tokens` - JWT refresh tokens
- `verification_tokens` - Email verification OTPs
- `user_devices` - Trusted devices per user
- `device_verification_tokens` - Device verification OTPs

### Migrations
Located in `src/main/resources/db/migration/`:
- `V1__init_schema.sql` - Initial schema
- `V2__add_email_verification.sql` - Email verification
- `V3__add_device_verification.sql` - Device tracking

## Security Features

### Rate Limiting
| Endpoint | Limit | Window |
|----------|-------|--------|
| Register | 3 requests | 5 minutes |
| Login | 3 requests | 5 minutes |
| Resend OTP | 3 requests | 5 minutes |

### Token Expiration
- Access Token: 15 minutes
- Refresh Token: 7 days
- Email OTP: 15 minutes
- Device OTP: 10 minutes

## Development

### Run tests:
```bash
./gradlew test
```

### Build without tests:
```bash
./gradlew build -x test
```

### Clean build:
```bash
./gradlew clean build
```

## Troubleshooting

### Email not sending
- Check MAIL_USERNAME and MAIL_PASSWORD in .env
- For Gmail, use App Password (not regular password)
- Verify SMTP settings for your provider

### Database connection failed
- Verify DB_URL, DB_USERNAME, DB_PASSWORD
- Check database is running and accessible
- Ensure SSL mode matches your database config

### Rate limit errors
- Wait for the rate limit window to expire (5 minutes)
- Check if IP is being correctly detected
- Review RateLimitService configuration

## License

[Your License Here]
