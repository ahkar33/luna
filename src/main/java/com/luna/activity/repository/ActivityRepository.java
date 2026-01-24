package com.luna.activity.repository;

import com.luna.activity.entity.Activity;
import com.luna.activity.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    Page<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    Page<Activity> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType, Pageable pageable);
    
    Page<Activity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
        Long userId, ActivityType activityType, Pageable pageable);
    
    Page<Activity> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
}
