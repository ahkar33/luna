package com.luna.notification.service.impl;

import com.google.firebase.messaging.*;
import com.luna.notification.dto.NotificationPayload;
import com.luna.notification.entity.UserFcmToken;
import com.luna.notification.repository.UserFcmTokenRepository;
import com.luna.notification.service.IFcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmServiceImpl implements IFcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserFcmTokenRepository userFcmTokenRepository;

    @Override
    public void sendToUser(Long userId, NotificationPayload payload) {
        List<UserFcmToken> tokens = userFcmTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No FCM tokens found for user {}, skipping notification", userId);
            return;
        }

        List<String> tokenStrings = tokens.stream()
                .map(UserFcmToken::getFcmToken)
                .toList();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenStrings)
                .setNotification(Notification.builder()
                        .setTitle(payload.title())
                        .setBody(payload.body())
                        .build())
                .putAllData(payload.data())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.debug("FCM multicast sent to user {}: {} success, {} failure",
                    userId, response.getSuccessCount(), response.getFailureCount());

            removeStaleTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to user {}", userId, e);
        }
    }

    private void removeStaleTokens(List<UserFcmToken> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    String staleToken = tokens.get(i).getFcmToken();
                    userFcmTokenRepository.deleteByFcmToken(staleToken);
                    log.info("Removed stale FCM token for user {}", tokens.get(i).getUser().getId());
                }
            }
        }
    }
}
