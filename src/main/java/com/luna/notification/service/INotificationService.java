package com.luna.notification.service;

import com.luna.notification.dto.RegisterFcmTokenRequest;

public interface INotificationService {

    void registerToken(Long userId, RegisterFcmTokenRequest request);

    void unregisterToken(String fcmToken);

    void sendFollowNotification(Long followerId, Long followedUserId);
}
