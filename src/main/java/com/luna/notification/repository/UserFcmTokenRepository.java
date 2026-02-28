package com.luna.notification.repository;

import com.luna.notification.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    List<UserFcmToken> findByUserId(Long userId);

    Optional<UserFcmToken> findByFcmToken(String fcmToken);

    void deleteByFcmToken(String fcmToken);
}
