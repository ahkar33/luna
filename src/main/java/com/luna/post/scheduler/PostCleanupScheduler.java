package com.luna.post.scheduler;

import com.luna.post.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "app.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PostCleanupScheduler {
    
    private final IPostService postService;
    
    /**
     * Permanently deletes posts that have been soft-deleted for more than 30 days.
     * Runs daily at 2:00 AM server time.
     * 
     * Cron format: second minute hour day month weekday
     * "0 0 2 * * *" = At 02:00:00 every day
     */
    @Scheduled(cron = "${app.scheduling.post-cleanup-cron:0 0 2 * * *}")
    public void cleanupOldDeletedPosts() {
        log.info("Starting scheduled cleanup of posts deleted more than 30 days ago");
        
        try {
            int deletedCount = postService.cleanupOldDeletedPosts();
            
            if (deletedCount > 0) {
                log.info("Cleanup completed successfully. Permanently deleted {} post(s)", deletedCount);
            } else {
                log.debug("Cleanup completed. No posts to delete");
            }
            
        } catch (Exception e) {
            log.error("Error during post cleanup job", e);
            // Don't rethrow - let scheduler continue running
        }
    }
}
