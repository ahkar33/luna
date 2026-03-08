package com.luna.user.repository;

import com.luna.user.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByOtpAndUserId(String otp, UUID userId);
    Optional<VerificationToken> findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID userId, Instant createdAfter);

    @Transactional
    int deleteByCreatedAtBefore(Instant cutoff);
}
