package com.luna.activity.repository;

import com.luna.activity.entity.Activity;
import com.luna.activity.entity.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Page<Activity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Activity> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType, Pageable pageable);

    Page<Activity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
        UUID userId, ActivityType activityType, Pageable pageable);

    Page<Activity> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId, Pageable pageable);
}
