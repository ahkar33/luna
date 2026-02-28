package com.luna.notification.service.impl;

import com.luna.common.exception.ResourceNotFoundException;
import com.luna.notification.dto.NotificationPayload;
import com.luna.notification.dto.RegisterFcmTokenRequest;
import com.luna.notification.entity.UserFcmToken;
import com.luna.notification.repository.UserFcmTokenRepository;
import com.luna.notification.service.IFcmService;
import com.luna.notification.service.INotificationService;
import com.luna.user.entity.User;
import com.luna.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private static final String FOLLOW_COOLDOWN_KEY = "notification:follow:cooldown:%d:%d";
    private static final long FOLLOW_COOLDOWN_HOURS = 1;

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserRepository userRepository;
    private final IFcmService fcmService;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void registerToken(Long userId, RegisterFcmTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<UserFcmToken> existing = userFcmTokenRepository.findByFcmToken(request.getFcmToken());

        if (existing.isPresent()) {
            // Token already exists — update user reference and metadata (lastUsedAt auto-updates)
            UserFcmToken token = existing.get();
            token.setUser(user);
            token.setPlatform(request.getPlatform());
            token.setDeviceName(request.getDeviceName());
            userFcmTokenRepository.save(token);
        } else {
            UserFcmToken token = UserFcmToken.builder()
                    .user(user)
                    .fcmToken(request.getFcmToken())
                    .platform(request.getPlatform())
                    .deviceName(request.getDeviceName())
                    .build();
            userFcmTokenRepository.save(token);
        }

        log.debug("FCM token registered for user {}", userId);
    }

    @Override
    @Transactional
    public void unregisterToken(String fcmToken) {
        userFcmTokenRepository.deleteByFcmToken(fcmToken);
        log.debug("FCM token unregistered: {}", fcmToken);
    }

    @Override
    @Async
    public void sendFollowNotification(Long followerId, Long followedUserId) {
        try {
            String cooldownKey = String.format(FOLLOW_COOLDOWN_KEY, followerId, followedUserId);

            Boolean alreadySent = redisTemplate.hasKey(cooldownKey);
            if (Boolean.TRUE.equals(alreadySent)) {
                log.debug("Follow notification suppressed (cooldown active): {} -> {}", followerId, followedUserId);
                return;
            }

            redisTemplate.opsForValue().set(cooldownKey, "1", FOLLOW_COOLDOWN_HOURS, TimeUnit.HOURS);

            User follower = userRepository.findById(followerId).orElse(null);
            if (follower == null) {
                log.warn("Follower not found for notification: {}", followerId);
                return;
            }

            String senderName = follower.getDisplayName() != null
                    ? follower.getDisplayName()
                    : follower.getUsernameField();

            NotificationPayload payload = new NotificationPayload(
                    senderName + " started following you",
                    "",
                    Map.of(
                            "type", "FOLLOW",
                            "userId", String.valueOf(followerId)
                    )
            );

            fcmService.sendToUser(followedUserId, payload);

        } catch (Exception e) {
            log.error("Failed to send follow notification: {} -> {}", followerId, followedUserId, e);
            // Don't throw — notification failure must never break the follow flow
        }
    }
}
