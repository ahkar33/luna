package com.luna.user.repository;

import com.luna.user.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByOtpAndUserId(String otp, Long userId);
    Optional<VerificationToken> findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, Instant createdAfter);
}
