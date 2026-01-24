package com.luna.activity.service;

import com.luna.activity.dto.ActivityResponse;
import com.luna.activity.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IActivityService {
    
    void logActivity(Long userId, ActivityType activityType, String entityType, 
                    Long entityId, Long targetUserId, String metadata);
    
    Page<ActivityResponse> getUserActivities(Long userId, Pageable pageable);
    
    Page<ActivityResponse> getUserActivitiesByType(Long userId, ActivityType activityType, Pageable pageable);
    
    Page<ActivityResponse> getActivitiesForUser(Long targetUserId, Pageable pageable);
}
