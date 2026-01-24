package com.luna.activity.dto;

import com.luna.activity.entity.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    
    private Long id;
    private UserInfo user;
    private ActivityType activityType;
    private String entityType;
    private Long entityId;
    private Long targetUserId;
    private String metadata;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
    }
}
