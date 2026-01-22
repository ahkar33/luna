package com.luna.auth.controller;

import com.luna.auth.dto.*;
import com.luna.auth.service.IAuthService;
import com.luna.common.service.RateLimitService;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        @ApiResponse(responseCode = "200", description = "Registration successful",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "429", description = "Too many requests",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP
        String key = "register:" + getClientIP(httpRequest);
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many registration attempts. Please try again later."));
        }
        
        authService.register(request);
        return ResponseEntity.ok(new MessageResponse(
            "Registration successful. Please check your email for verification code."
        ));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address", description = "Verifies user email using the OTP sent during registration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content)
    })
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(new MessageResponse(
            "Email verified successfully. You can now login."
        ));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP", description = "Resends the email verification OTP to the user's email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP sent successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "429", description = "Too many requests",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<?> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP + email
        String key = "resend:" + getClientIP(httpRequest) + ":" + request.getEmail();
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many requests. Please try again later."));
        }
        
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(new MessageResponse(
            "Verification code sent to your email."
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT tokens. May require device verification for new devices.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful", 
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "429", description = "Too many login attempts",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthRequest request,
            @Parameter(description = "Device fingerprint for device verification")
            @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint,
            HttpServletRequest httpRequest) {
        
        // Rate limit by IP + email (prevent brute force)
        String key = "login:" + getClientIP(httpRequest) + ":" + request.getEmail();
        Bucket bucket = rateLimitService.resolveBucket(key);
        
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many login attempts. Please try again later."));
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
    @Operation(summary = "Verify new device", description = "Verifies a new device using the OTP sent to user's email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device verified successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content)
    })
    public ResponseEntity<AuthResponse> verifyDevice(@Valid @RequestBody VerifyDeviceRequest request) {
        return ResponseEntity.ok(authService.verifyDevice(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content)
    })
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
