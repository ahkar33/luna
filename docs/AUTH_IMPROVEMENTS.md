# Authentication System Improvements

This document tracks security issues, code quality improvements, and missing features in the Luna authentication system.

## Phase 1: Critical Security (Immediate)

- [x] **1. Fix weak OTP generation** `CRITICAL`
  Uses `SecureRandom` instead of `Random` in `AuthServiceImpl.java`

- [ ] **2. Add OTP brute force protection** `CRITICAL`
  ~~Unlimited attempts to guess 6-digit OTPs on `verifyEmail`, `verifyDevice`, `verifyResetPasswordOtp`.~~
  Rate limiting added to `/verify-email` and `/verify-device` (20 req/min for dev). Still TODO:
  - Tighten to 3-5 attempts per 5 min for production
  - Add failed attempt tracking at service level (invalidate OTP after N wrong guesses)
  - Add temporary lockout

- [x] **3. Fix inefficient device OTP resend query** `HIGH`
  Replaced `findAll()` + in-memory filter with `findFirstByUserIdOrderByCreatedAtDesc(userId)` repository query.

- [x] **4. Add token cleanup job** `HIGH`
  `TokenCleanupScheduler` runs daily at 3 AM, deletes tokens older than 24 hours from all three token tables.

- [x] **5. Add database indices** `HIGH`
  Indices exist on verification_tokens, device_verification_tokens, password_reset_tokens, and refresh_tokens tables via Flyway migrations.

## Phase 2: High Priority Security & Features

- [x] **6. Implement refresh token rotation** `CRITICAL`
  Old refresh token is revoked and new one generated on each use in `AuthServiceImpl.java`.

- [ ] **7. Add account lockout mechanism** `HIGH`
  No protection after N failed login attempts.
  Lock account for 30 minutes after 5-10 failed attempts within 1 hour.

- [ ] **8. Implement proper rate limiting per endpoint** `HIGH`
  Currently 20 req/min for all endpoints. Need different limits:
  - Login: 5 attempts per 15 min per IP+email
  - Password reset: 3 per hour
  - OTP verification: 3 per 5 min
  - Registration: 3 per hour per IP

- [ ] **9. Add logout endpoint** `HIGH`
  No `/api/auth/logout` endpoint. Users can't invalidate tokens.
  Revoke refresh token and optionally blocklist access token.

- [ ] **10. Add audit logging** `HIGH`
  No structured logging for security events (failed logins, password changes, device additions).
  Log all security events with IP, timestamp, and user info.

## Phase 3: Token Revocation & Caching

- [ ] **11. Add JWT `jti` claim and token blocklist** `HIGH`
  No `jti` claim in JwtService.java, can't invalidate specific access tokens.
  Add `jti` claim and implement Redis-backed token blocklist.

- [ ] **12. Implement user caching** `MEDIUM`
  `JwtAuthenticationFilter.java` loads user from DB on every authenticated request.
  Cache UserDetails in Redis with TTL matching access token expiry.

- [ ] **13. Fix distributed rate limiting** `HIGH`
  `RateLimitService.java` uses `ConcurrentHashMap` - won't work with multiple instances.
  Replace with Redis-backed rate limiting (Bucket4j Redis integration).

## Phase 4: UX & Additional Features

- [ ] **14. Add session/device management** `MEDIUM`
  Users can't view active devices or revoke access.
  Add `GET /api/auth/devices` and `DELETE /api/auth/devices/{id}`.

- [x] **15. Implement password strength validation** `HIGH`
  `RegisterRequest` and `ResetPasswordRequest` now require min 8 chars with uppercase, lowercase, number, and special character.
  `AuthRequest` (login) unchanged so old users with weaker passwords can still log in.

- [x] **16. Use generic error messages (prevent enumeration)** `MEDIUM`
  `GlobalExceptionHandler` returns "Invalid email or password" for both `BadCredentials` and `UsernameNotFound` exceptions.

- [ ] **17. Add email change flow** `MEDIUM`
  Users can't change their email. Add OTP verification on old and new email.

- [ ] **18. Add "trust device" option** `MEDIUM`
  Device verification required every time on new device.
  Add "Trust this device for 30 days" option.

## Phase 5: Nice-to-Have

- [ ] **19. Add password history checking** `LOW`
  Users can reuse old passwords. Store hashes of last 5 passwords, prevent reuse.

- [ ] **20. Add suspicious activity detection** `LOW`
  No detection of unusual login patterns. Alert on login from new country, many IPs, or many failed attempts.

- [ ] **21. Add account deletion** `MEDIUM`
  No account deletion endpoint (GDPR concern).
  Add `/api/auth/delete-account` with password + OTP confirmation and soft delete cascade.

- [ ] **22. Move hardcoded expiry times to config** `MEDIUM`
  JWT expiry is in `application.yml`, but OTP expiry times are hardcoded (900s, 600s, 900s).
  Move all to `application.yml`.

- [ ] **23. Refactor duplicate rate limiting code** `MEDIUM`
  Same rate limiting pattern repeated in every AuthController method.
  Extract to custom annotation `@RateLimit("login")` or interceptor.

## Other Issues

- [ ] **24. Weak device fingerprint fallback** `MEDIUM`
  `AuthController.java` falls back to `"unknown-" + IP` which is easily spoofed.
  Require proper fingerprint or use more robust fallback.

- [ ] **25. Rate limit cache never expires** `MEDIUM`
  `ConcurrentHashMap` buckets stay in memory indefinitely (memory leak).
  Add TTL or use Caffeine cache with eviction.

- [ ] **26. Remove unused token field** `LOW`
  `VerificationToken.java` has both `token` UUID and `otp`, but only OTP is used.
  Remove unused `token` field or use it for URL-based verification.

- [ ] **27. Standardize OTP expiry times** `LOW`
  Email verification: 15 min, Device verification: 10 min, Password reset: 15 min.
  Standardize to 15 minutes for all.

- [ ] **28. Add rate limit user feedback** `LOW`
  Error just says "too many requests" without retry-after time.
  Include seconds to wait in response.

- [x] **29. CORS configuration** `MEDIUM`
  Properly configured in `CorsConfig.java` with allowed origins, methods, headers, and credentials.

- [ ] **30. Add password reset link option** `LOW`
  Only OTP-based reset. Optionally add link-based reset (token in URL) as alternative.

- [ ] **31. JWT secret validation** `MEDIUM`
  No runtime validation of JWT secret length.
  Add `@PostConstruct` validation that secret is at least 256 bits.

- [x] **32. Two-step password reset** `LOW`
  Implemented with `verified` column on password_reset_tokens (V15 migration).
  Step 1: verify OTP, Step 2: reset password with verified token.

---

**Progress: 9/32 completed**
