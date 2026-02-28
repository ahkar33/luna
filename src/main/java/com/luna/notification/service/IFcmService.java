package com.luna.notification.service;

import com.luna.notification.dto.NotificationPayload;

public interface IFcmService {

    void sendToUser(Long userId, NotificationPayload payload);
}
