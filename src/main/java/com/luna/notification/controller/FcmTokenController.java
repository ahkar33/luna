package com.luna.notification.controller;

import com.luna.common.dto.ApiResponse;
import com.luna.notification.dto.RegisterFcmTokenRequest;
import com.luna.notification.service.INotificationService;
import com.luna.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/fcm-tokens")
@RequiredArgsConstructor
@Tag(name = "FCM Tokens", description = "Manage device FCM tokens for push notifications")
@SecurityRequirement(name = "bearerAuth")
public class FcmTokenController {

    private final INotificationService notificationService;

    @PostMapping
    @Operation(
            summary = "Register FCM token",
            description = "Register a device FCM token for the authenticated user to receive push notifications"
    )
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @Valid @RequestBody RegisterFcmTokenRequest request,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        notificationService.registerToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success("FCM token registered successfully"));
    }

    @DeleteMapping("/{token}")
    @Operation(
            summary = "Unregister FCM token",
            description = "Remove a device FCM token, call this on logout or app uninstall"
    )
    public ResponseEntity<ApiResponse<Void>> unregisterToken(@PathVariable("token") String token) {
        notificationService.unregisterToken(token);
        return ResponseEntity.ok(ApiResponse.success("FCM token unregistered successfully"));
    }
}
