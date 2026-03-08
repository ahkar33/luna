package com.luna.notification.repository;

import com.luna.notification.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, UUID> {

    List<UserFcmToken> findByUserId(UUID userId);

    Optional<UserFcmToken> findByFcmToken(String fcmToken);

    void deleteByFcmToken(String fcmToken);
}
