package com.luna.notification.service;

import com.luna.notification.dto.NotificationPayload;

import java.util.UUID;

public interface IFcmService {

    void sendToUser(UUID userId, NotificationPayload payload);
}
