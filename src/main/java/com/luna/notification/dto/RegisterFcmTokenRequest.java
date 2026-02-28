package com.luna.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterFcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    private String platform;

    private String deviceName;
}
