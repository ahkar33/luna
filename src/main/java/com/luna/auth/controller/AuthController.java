package com.luna.auth.controller;

import com.luna.auth.dto.*;
import com.luna.auth.service.IAuthService;
import com.luna.common.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP
        String key = "register:" + getClientIP(httpRequest);
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many registration attempts. Please try again later."));
        }
        
        authService.register(request);
        return ResponseEntity.ok(Map.of(
            "message", "Registration successful. Please check your email for verification code."
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(Map.of(
            "message", "Email verified successfully. You can now login."
        ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP + email
        String key = "resend:" + getClientIP(httpRequest) + ":" + request.getEmail();
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please try again later."));
        }
        
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "Verification code sent to your email."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthRequest request,
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP + email (prevent brute force)
        String key = "login:" + getClientIP(httpRequest) + ":" + request.getEmail();
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please try again later."));
        }

        // Default device fingerprint if not provided
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            deviceFingerprint = "unknown-" + getClientIP(httpRequest);
        }

        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.login(request, deviceFingerprint, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-device")
    public ResponseEntity<AuthResponse> verifyDevice(@Valid @RequestBody VerifyDeviceRequest request) {
        return ResponseEntity.ok(authService.verifyDevice(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
