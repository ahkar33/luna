package com.luna.user.repository;

import com.luna.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByOtpAndUserId(String otp, UUID userId);

    Optional<PasswordResetToken> findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID userId, Instant after);

    Optional<PasswordResetToken> findFirstByUserIdAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    @Transactional
    int deleteByCreatedAtBefore(Instant cutoff);
}
