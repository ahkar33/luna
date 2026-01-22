package com.luna.auth.controller;

import com.luna.auth.dto.*;
import com.luna.auth.service.IAuthService;
import com.luna.common.dto.ApiResponse;
import com.luna.common.exception.BadRequestException;
import com.luna.common.service.RateLimitService;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final IAuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and sends a verification email")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP
        String key = "register:" + getClientIP(httpRequest);
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            throw new BadRequestException("Too many registration attempts. Please try again later.");
        }
        
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(
            "Registration successful. Please check your email for verification code."
        ));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address", description = "Verifies user email using the OTP sent during registration")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(
            "Email verified successfully. You can now login."
        ));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP", description = "Resends the email verification OTP to the user's email")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Object>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP + email
        String key = "resend:" + getClientIP(httpRequest) + ":" + request.getEmail();
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            throw new BadRequestException("Too many requests. Please try again later.");
        }
        
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "Verification code sent to your email."
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT tokens. May require device verification for new devices.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many login attempts"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody AuthRequest request,
            @Parameter(description = "Device fingerprint for device verification")
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP + email (prevent brute force)
        String key = "login:" + getClientIP(httpRequest) + ":" + request.getEmail();
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            throw new BadRequestException("Too many login attempts. Please try again later.");
        }

        // Default device fingerprint if not provided
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            deviceFingerprint = "unknown-" + getClientIP(httpRequest);
        }

        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.login(request, deviceFingerprint, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/verify-device")
    @Operation(summary = "Verify new device", description = "Verifies a new device using the OTP sent to user's email")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> verifyDevice(@Valid @RequestBody VerifyDeviceRequest request) {
        AuthResponse response = authService.verifyDevice(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Device verified successfully"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
