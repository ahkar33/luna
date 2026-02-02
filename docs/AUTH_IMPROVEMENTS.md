# Authentication System Improvements

This document tracks security issues, code quality improvements, and missing features in the Luna authentication system.

## Critical Security Issues

### 1. Weak OTP Generation
**File:** `AuthServiceImpl.java:302-306`
**Priority:** CRITICAL
**Issue:** Uses `Random` instead of `SecureRandom`, making OTPs predictable
**Impact:** Attackers can potentially predict OTPs
**Fix:** Replace `Random` with `SecureRandom`

### 2. No Refresh Token Rotation
**File:** `AuthServiceImpl.java:196-210`
**Priority:** CRITICAL
**Issue:** Refresh tokens are reused instead of rotating on each use
**Impact:** If stolen, attacker has 7 days of access without detection
**Fix:** Implement refresh token rotation - generate new refresh token on each use and invalidate old one

### 3. No Brute Force Protection on OTP Verification
**Files:** `verifyEmail`, `verifyDevice`, `verifyResetPasswordOtp` methods
**Priority:** CRITICAL
**Issue:** Unlimited attempts to guess 6-digit OTPs (only 1 million combinations)
**Impact:** Attackers can brute force OTP codes
**Fix:** Add rate limiting on OTP verification (3-5 attempts max) and temporary lockout

### 4. No Access Token Revocation Mechanism
**File:** `JwtService.java`
**Priority:** HIGH
**Issue:** JWT has no `jti` (token ID) claim, can't invalidate specific tokens
**Impact:** No logout functionality, compromised tokens valid until expiry
**Fix:** Add `jti` claim and implement token blocklist (Redis) or reduce expiry time significantly

### 5. Inefficient Device OTP Resend Query
**File:** `AuthServiceImpl.java:287-292`
**Priority:** HIGH
**Issue:** Loads ALL device verification tokens via `findAll()` then filters in memory
**Impact:** Potential DoS vector, memory issues with many tokens
**Fix:** Add proper repository query: `findFirstByUserIdOrderByCreatedAtDesc(userId)`

### 6. Weak Device Fingerprint Fallback
**File:** `AuthController.java:114-116`
**Priority:** MEDIUM
**Issue:** Falls back to `"unknown-" + IP` which is easily spoofed
**Impact:** Device verification can be bypassed
**Fix:** Require proper fingerprint or reject request, or use more robust fallback

## High Priority Improvements

### 7. Rate Limiting Too Permissive
**File:** `RateLimitService.java:22-24`
**Priority:** HIGH
**Issue:** 20 requests/minute for all auth endpoints is too high
**Impact:** Brute force attacks are still feasible
**Fix:** Different limits per endpoint:
- Login: 5 attempts per 15 minutes per IP+email
- Password reset: 3 attempts per hour
- OTP verification: 3 attempts per 5 minutes
- Registration: 3 per hour per IP

### 8. No Token Cleanup
**Priority:** HIGH
**Issue:** Expired OTPs and verification tokens accumulate in database
**Impact:** Database bloat, performance degradation
**Fix:** Add scheduled job to delete tokens older than 24 hours (similar to PostCleanupScheduler)

### 9. Missing Account Lockout
**Priority:** HIGH
**Issue:** No protection after N failed login attempts
**Impact:** Unlimited brute force attempts per rate limit window
**Fix:** Lock account for 30 minutes after 5-10 failed login attempts within 1 hour

### 10. No Audit Logging
**Priority:** HIGH
**Issue:** Failed login attempts, password changes, device additions not tracked
**Impact:** No security monitoring or forensics capability
**Fix:** Log all security events:
- Failed login attempts (with IP, timestamp)
- Successful logins from new devices
- Password changes
- OTP requests
- Account lockouts

## Code Quality Issues

### 11. Duplicate Rate Limiting Logic
**File:** `AuthController.java` (multiple methods)
**Priority:** MEDIUM
**Issue:** Same rate limiting pattern repeated in every controller method
**Impact:** Code duplication, maintenance burden
**Fix:** Extract to custom annotation `@RateLimit("login")` or use interceptor

### 12. Hardcoded Expiry Times
**Files:** `AuthServiceImpl.java:216, 313, 366, 394`
**Priority:** MEDIUM
**Issue:** Token expiry times hardcoded (900, 600 seconds)
**Impact:** Can't adjust without code changes
**Fix:** Move to `application.yml`:
```yaml
app.security:
  otp-expiry-minutes: 15
  device-otp-expiry-minutes: 10
  password-reset-expiry-minutes: 15
```

### 13. Inconsistent Token Expiry
**Files:** Email verification: 15 min, Device verification: 10 min
**Priority:** LOW
**Issue:** No clear reason for different expiry times
**Impact:** User confusion
**Fix:** Standardize to 15 minutes for all OTP types

### 14. Missing Database Indices
**Files:** All token entity tables
**Priority:** HIGH
**Issue:** No indices on frequently queried columns
**Impact:** Slow queries as user base grows
**Fix:** Add indices:
```sql
CREATE INDEX idx_verification_tokens_user_otp ON verification_tokens(user_id, otp);
CREATE INDEX idx_verification_tokens_created ON verification_tokens(created_at);
CREATE INDEX idx_device_tokens_user_otp ON device_verification_tokens(user_id, otp, device_fingerprint);
CREATE INDEX idx_password_reset_user_otp ON password_reset_tokens(user_id, otp);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

### 15. No User Caching
**File:** `JwtAuthenticationFilter.java:44`
**Priority:** MEDIUM
**Issue:** Loads user from database on every authenticated request
**Impact:** Unnecessary database load
**Fix:** Cache UserDetails in Redis with TTL matching access token expiry

### 16. Unused Token Field
**File:** `VerificationToken.java:23-24`
**Priority:** LOW
**Issue:** Has both `token` UUID and `otp`, but only OTP is used
**Impact:** Wasted storage
**Fix:** Remove unused `token` field or use it for URL-based verification

### 17. No Rate Limit User Feedback
**Files:** All rate limiting error messages
**Priority:** LOW
**Issue:** Just says "too many requests" without retry-after time
**Impact:** Poor UX
**Fix:** Include seconds to wait: "Too many attempts. Please try again in 47 seconds."

## Missing Features

### 18. No Logout Endpoint
**Priority:** HIGH
**Issue:** Users can't invalidate their tokens
**Impact:** Stolen tokens remain valid until expiry
**Fix:** Add `/api/auth/logout` endpoint that:
- Revokes refresh token
- Adds access token to blocklist (if implementing token blocklist)

### 19. No Session Management
**Priority:** MEDIUM
**Issue:** Users can't view active devices/sessions or revoke access
**Impact:** No control over security after compromise
**Fix:** Add endpoints:
- `GET /api/auth/devices` - List all user devices
- `DELETE /api/auth/devices/{id}` - Revoke device access
- Show last login time, location, device info

### 20. No Password Strength Validation
**Priority:** HIGH
**Issue:** Only relies on `@Valid` annotation
**Impact:** Weak passwords allowed
**Fix:** Implement password policy:
- Minimum 8 characters
- At least 1 uppercase, 1 lowercase, 1 number
- Check against common passwords list
- Reject passwords containing username/email

### 21. User Enumeration via Error Messages
**File:** `AuthServiceImpl.java:94-95`
**Priority:** MEDIUM
**Issue:** Different errors reveal if email exists ("User not found" vs "Email not verified")
**Impact:** Attackers can enumerate valid email addresses
**Fix:** Return generic "Invalid credentials" for all login failures

### 22. No Email Change Flow
**Priority:** MEDIUM
**Issue:** Users can't change their email address
**Impact:** Poor UX, users must create new account
**Fix:** Add email change flow:
- Verify old email with OTP
- Verify new email with OTP
- Update email after both verified

### 23. No Password History
**Priority:** LOW
**Issue:** Users can reuse old passwords
**Impact:** Reduced security if password compromised
**Fix:** Store hashes of last 5 passwords, prevent reuse

### 24. No Account Deletion
**Priority:** MEDIUM
**Issue:** Users can't delete their accounts
**Impact:** GDPR compliance issue
**Fix:** Add `/api/auth/delete-account` with:
- Password confirmation
- OTP verification
- Soft delete user and cascade to related data

### 25. No Suspicious Activity Detection
**Priority:** LOW
**Issue:** No detection of unusual login patterns
**Impact:** Missed opportunities to alert users of compromises
**Fix:** Alert user via email when:
- Login from new country
- Login from many different IPs in short time
- Many failed login attempts

### 26. No "Remember Me" or "Trust Device" Option
**Priority:** MEDIUM
**Issue:** Device verification required every time on new device
**Impact:** Annoying UX for legitimate users
**Fix:** Add "Trust this device for 30 days" checkbox that extends device validity

## Architecture Concerns

### 27. In-Memory Rate Limiting Won't Scale
**File:** `RateLimitService.java:14`
**Priority:** HIGH (if planning multi-instance deployment)
**Issue:** `ConcurrentHashMap` won't work with multiple instances
**Impact:** Rate limiting ineffective in distributed setup
**Fix:** Replace with Redis-backed rate limiting using Bucket4j's Redis integration

### 28. Rate Limit Cache Never Expires
**File:** `RateLimitService.java:14`
**Priority:** MEDIUM
**Issue:** Bucket cache grows indefinitely
**Impact:** Memory leak
**Fix:** Add TTL to cache entries or use Caffeine cache with eviction policy

### 29. Two-Step Password Reset Adds Complexity
**Files:** `/verify-reset-password-otp` and `/reset-password` endpoints
**Priority:** LOW
**Issue:** Requires two API calls (verify OTP, then reset password)
**Impact:** More complex client implementation
**Fix:** Consider single endpoint that accepts OTP + new password together

### 30. No CORS Configuration Visibility
**Priority:** MEDIUM
**Issue:** Not clear if CORS is properly configured for auth endpoints
**Impact:** Potential security issues or broken frontend
**Fix:** Document CORS settings and ensure proper configuration

### 31. No Password Reset Link Option
**Priority:** LOW
**Issue:** Only OTP-based reset, no email link option
**Impact:** Less flexible UX
**Fix:** Optionally add link-based reset (token in URL) as alternative to OTP

### 32. JWT Secret Validation
**File:** `JwtService.java:76-78`
**Priority:** MEDIUM
**Issue:** No runtime validation of JWT secret length
**Impact:** Weak secrets could be used in development
**Fix:** Add `@PostConstruct` validation that JWT secret is at least 256 bits

## Implementation Priority

### Phase 1: Critical Security (Immediate)
1. Fix weak OTP generation (SecureRandom)
2. Add OTP brute force protection
3. Fix inefficient device OTP query
4. Add token cleanup job
5. Add database indices

### Phase 2: High Priority Security & Features
6. Implement refresh token rotation
7. Add account lockout mechanism
8. Implement proper rate limiting per endpoint
9. Add logout endpoint
10. Add audit logging

### Phase 3: Token Revocation & Caching
11. Add JWT `jti` claim and token blocklist
12. Implement user caching in Redis
13. Fix distributed rate limiting

### Phase 4: UX & Additional Features
14. Add session/device management
15. Implement password strength validation
16. Add generic error messages (prevent enumeration)
17. Add email change flow
18. Add "trust device" option

### Phase 5: Nice-to-Have
19. Add password history checking
20. Add suspicious activity detection
21. Add account deletion
22. Configurable token expiry times
23. Refactor duplicate rate limiting code
