# Device Verification & Login Security

## Features

### 1. Login Rate Limiting (Brute Force Protection)
- **3 login attempts per 5 minutes** per IP + email combination
- Prevents password guessing attacks
- Returns 429 Too Many Requests when limit exceeded

### 2. Device Verification (New Device Detection)
- Detects logins from new/unverified devices
- Sends 6-digit OTP to user's email
- Requires verification before granting access

## Login Flow

### Known Device (Normal Login)
```bash
POST /api/auth/login
Content-Type: application/json
X-Device-Fingerprint: abc123xyz

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (Success):**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "requiresDeviceVerification": false
}
```

### New Device (Requires Verification)
```bash
POST /api/auth/login
Content-Type: application/json
X-Device-Fingerprint: new-device-xyz

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (Verification Required):**
```json
{
  "requiresDeviceVerification": true,
  "message": "New device detected. Please check your email for verification code."
}
```

User receives email with 6-digit OTP (valid for 10 minutes).

### Verify New Device
```bash
POST /api/auth/verify-device
Content-Type: application/json

{
  "email": "user@example.com",
  "deviceFingerprint": "new-device-xyz",
  "otp": "123456"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Device Fingerprinting

### What is a Device Fingerprint?
A unique identifier for a device/browser combination. Can be generated using:

**Client-side (JavaScript):**
```javascript
// Simple approach
const fingerprint = btoa(
  navigator.userAgent + 
  navigator.language + 
  screen.width + 
  screen.height
);

// Or use a library like FingerprintJS
import FingerprintJS from '@fingerprintjs/fingerprintjs';

const fp = await FingerprintJS.load();
const result = await fp.get();
const fingerprint = result.visitorId;
```

**Send with login request:**
```javascript
fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Device-Fingerprint': fingerprint
  },
  body: JSON.stringify({ email, password })
});
```

### Fallback
If no fingerprint is provided, the system uses `"unknown-" + IP_ADDRESS` as fallback.

## Security Features

### Rate Limiting
| Endpoint | Limit | Window | Key |
|----------|-------|--------|-----|
| `/register` | 3 requests | 5 minutes | IP |
| `/login` | 3 requests | 5 minutes | IP + Email |
| `/resend-otp` | 3 requests | 5 minutes | IP + Email |

### Device Tracking
- Stores device fingerprint, IP, user agent
- Tracks first seen, last seen, verification status
- Updates IP and last seen on each login
- Verified devices don't require OTP again

### OTP Security
- 6-digit random code (100000-999999)
- Expires in 10 minutes
- Can only be used once
- Tied to specific user + device combination

## Email Notifications

### New Device Login Email
```
Subject: Luna - New Device Login Detected

New Device Login Detected

We detected a login from a new device:
IP: 192.168.1.1
Browser: Mozilla/5.0...

Your verification code is: 123456

This code will expire in 10 minutes.

If this wasn't you, please change your password immediately.
```

## Database Schema

### user_devices
- Stores verified devices per user
- Unique constraint on (user_id, device_fingerprint)
- Tracks verification status and timestamps

### device_verification_tokens
- Temporary OTP storage
- Expires after 10 minutes
- Marked as used after verification

## Best Practices

1. **Generate stable fingerprints** - Use consistent device attributes
2. **Handle verification UI** - Show clear message when device verification is required
3. **Store fingerprint** - Save in localStorage/cookie after verification
4. **Monitor failed attempts** - Log and alert on suspicious activity
5. **Allow device management** - Let users view/revoke trusted devices (future feature)

## Testing

### Test New Device Flow
1. Login with new device fingerprint
2. Check email for OTP
3. Verify device with OTP
4. Login again with same fingerprint (should succeed without OTP)

### Test Rate Limiting
1. Attempt 4 logins in quick succession
2. 4th attempt should return 429 Too Many Requests
3. Wait 5 minutes and try again
