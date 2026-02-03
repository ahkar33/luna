package com.luna.activity.service.impl;

import com.luna.activity.dto.ActivityResponse;
import com.luna.activity.entity.Activity;
import com.luna.activity.entity.ActivityType;
import com.luna.activity.repository.ActivityRepository;
import com.luna.activity.service.IActivityService;
import com.luna.user.entity.User;
import com.luna.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements IActivityService {
    
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    
    @Override
    @Async
    @Transactional
    public void logActivity(Long userId, ActivityType activityType, String entityType, 
                           Long entityId, Long targetUserId, String metadata) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Cannot log activity for non-existent user: {}", userId);
                return;
            }
            
            Activity activity = Activity.builder()
                .user(user)
                .activityType(activityType)
                .entityType(entityType)
                .entityId(entityId)
                .targetUserId(targetUserId)
                .metadata(metadata)
                .build();
            
            activityRepository.save(activity);
            log.debug("Activity logged: {} by user {} on {} {}", 
                activityType, userId, entityType, entityId);
                
        } catch (Exception e) {
            log.error("Failed to log activity: {} for user {}", activityType, userId, e);
            // Don't throw - activity logging should not break main flow
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getUserActivities(Long userId, Pageable pageable) {
        Page<Activity> activities = activityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return activities.map(this::mapToActivityResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getUserActivitiesByType(Long userId, ActivityType activityType, Pageable pageable) {
        Page<Activity> activities = activityRepository
            .findByUserIdAndActivityTypeOrderByCreatedAtDesc(userId, activityType, pageable);
        return activities.map(this::mapToActivityResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getActivitiesForUser(Long targetUserId, Pageable pageable) {
        Page<Activity> activities = activityRepository
            .findByTargetUserIdOrderByCreatedAtDesc(targetUserId, pageable);
        return activities.map(this::mapToActivityResponse);
    }
    
    private ActivityResponse mapToActivityResponse(Activity activity) {
        return ActivityResponse.builder()
            .id(activity.getId())
            .user(ActivityResponse.UserInfo.builder()
                .id(activity.getUser().getId())
                .username(activity.getUser().getUsernameField())
                .email(activity.getUser().getEmail())
                .build())
            .activityType(activity.getActivityType())
            .entityType(activity.getEntityType())
            .entityId(activity.getEntityId())
            .targetUserId(activity.getTargetUserId())
            .metadata(activity.getMetadata())
            .createdAt(activity.getCreatedAt())
            .build();
    }
}
