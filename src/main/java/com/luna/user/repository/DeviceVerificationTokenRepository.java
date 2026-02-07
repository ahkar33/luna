package com.luna.user.repository;

import com.luna.user.entity.DeviceVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DeviceVerificationTokenRepository extends JpaRepository<DeviceVerificationToken, Long> {
    Optional<DeviceVerificationToken> findByOtpAndUserIdAndDeviceFingerprint(String otp, Long userId, String deviceFingerprint);
    Optional<DeviceVerificationToken> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Transactional
    int deleteByCreatedAtBefore(Instant cutoff);
}
