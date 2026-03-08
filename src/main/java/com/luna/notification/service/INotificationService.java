package com.luna.notification.service;

import com.luna.notification.dto.RegisterFcmTokenRequest;

import java.util.UUID;

public interface INotificationService {

    void registerToken(UUID userId, RegisterFcmTokenRequest request);

    void unregisterToken(String fcmToken);

    void sendFollowNotification(UUID followerId, UUID followedUserId);
}
