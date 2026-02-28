package com.luna.notification.controller;

import com.luna.auth.dto.MessageResponse;
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
    public ResponseEntity<MessageResponse> registerToken(
            @Valid @RequestBody RegisterFcmTokenRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        notificationService.registerToken(userId, request);
        return ResponseEntity.ok(new MessageResponse("FCM token registered successfully"));
    }

    @DeleteMapping("/{token}")
    @Operation(
            summary = "Unregister FCM token",
            description = "Remove a device FCM token, call this on logout or app uninstall"
    )
    public ResponseEntity<MessageResponse> unregisterToken(@PathVariable("token") String token) {
        notificationService.unregisterToken(token);
        return ResponseEntity.ok(new MessageResponse("FCM token unregistered successfully"));
    }
}
