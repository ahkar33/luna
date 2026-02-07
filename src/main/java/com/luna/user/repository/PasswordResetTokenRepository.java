package com.luna.user.repository;

import com.luna.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByOtpAndUserId(String otp, Long userId);

    Optional<PasswordResetToken> findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, Instant after);

    Optional<PasswordResetToken> findFirstByUserIdAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(Long userId);

    @Transactional
    int deleteByCreatedAtBefore(Instant cutoff);
}
