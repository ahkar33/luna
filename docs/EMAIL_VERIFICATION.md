# Email Verification Flow

## Overview
Users must verify their email with a 6-digit OTP code before they can login.

## Registration Flow

### 1. Register
```bash
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "johndoe",
  "password": "password123"
}
```

**Response:**
```json
{
  "message": "Registration successful. Please check your email for verification code."
}
```

User receives email with 6-digit OTP (valid for 15 minutes).

### 2. Verify Email
```bash
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response:**
```json
{
  "message": "Email verified successfully. You can now login."
}
```

### 3. Resend OTP (if expired)
```bash
POST /api/auth/resend-otp
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "message": "Verification code sent to your email."
}
```

### 4. Login (after verification)
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Email Configuration

### Gmail Setup
1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password: https://myaccount.google.com/apppasswords
3. Add to `.env`:
```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-16-char-app-password
```

### Other Email Providers
- **SendGrid**: `smtp.sendgrid.net:587`
- **Mailgun**: `smtp.mailgun.org:587`
- **AWS SES**: `email-smtp.region.amazonaws.com:587`

## Database Changes
- Added `email_verified` column to `users` table
- Created `verification_tokens` table for OTP storage
- OTP expires after 15 minutes
- Users are inactive until email is verified

## Security Features
- OTP is 6 digits (100000-999999)
- OTP expires in 15 minutes
- OTP can only be used once
- User account is inactive until verified
- Login blocked for unverified users
