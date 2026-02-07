package com.luna.auth.scheduler;

import com.luna.user.repository.DeviceVerificationTokenRepository;
import com.luna.user.repository.PasswordResetTokenRepository;
import com.luna.user.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "app.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TokenCleanupScheduler {

    private final VerificationTokenRepository verificationTokenRepository;
    private final DeviceVerificationTokenRepository deviceVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Deletes expired tokens older than 24 hours.
     * Runs daily at 3:00 AM server time (1 hour after post cleanup).
     *
     * Cron format: second minute hour day month weekday
     * "0 0 3 * * *" = At 03:00:00 every day
     */
    @Scheduled(cron = "${app.scheduling.token-cleanup-cron:0 0 3 * * *}")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired tokens");

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int totalDeleted = 0;

        try {
            int deleted = verificationTokenRepository.deleteByCreatedAtBefore(cutoff);
            totalDeleted += deleted;
            log.debug("Deleted {} email verification token(s)", deleted);
        } catch (Exception e) {
            log.error("Error cleaning up email verification tokens", e);
        }

        try {
            int deleted = deviceVerificationTokenRepository.deleteByCreatedAtBefore(cutoff);
            totalDeleted += deleted;
            log.debug("Deleted {} device verification token(s)", deleted);
        } catch (Exception e) {
            log.error("Error cleaning up device verification tokens", e);
        }

        try {
            int deleted = passwordResetTokenRepository.deleteByCreatedAtBefore(cutoff);
            totalDeleted += deleted;
            log.debug("Deleted {} password reset token(s)", deleted);
        } catch (Exception e) {
            log.error("Error cleaning up password reset tokens", e);
        }

        if (totalDeleted > 0) {
            log.info("Token cleanup completed. Deleted {} token(s) total", totalDeleted);
        } else {
            log.debug("Token cleanup completed. No tokens to delete");
        }
    }
}
