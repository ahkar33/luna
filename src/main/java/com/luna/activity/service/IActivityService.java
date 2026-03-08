package com.luna.activity.service;

import com.luna.activity.dto.ActivityResponse;
import com.luna.activity.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IActivityService {

    void logActivity(UUID userId, ActivityType activityType, String entityType,
                    UUID entityId, UUID targetUserId, String metadata);

    Page<ActivityResponse> getUserActivities(UUID userId, Pageable pageable);

    Page<ActivityResponse> getUserActivitiesByType(UUID userId, ActivityType activityType, Pageable pageable);

    Page<ActivityResponse> getActivitiesForUser(UUID targetUserId, Pageable pageable);
}
