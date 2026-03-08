package com.luna.user.repository;

import com.luna.user.entity.DeviceVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceVerificationTokenRepository extends JpaRepository<DeviceVerificationToken, UUID> {
    Optional<DeviceVerificationToken> findByOtpAndUserIdAndDeviceFingerprint(String otp, UUID userId, String deviceFingerprint);
    Optional<DeviceVerificationToken> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Transactional
    int deleteByCreatedAtBefore(Instant cutoff);
}
