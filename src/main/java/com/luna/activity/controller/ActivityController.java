package com.luna.activity.controller;

import com.luna.activity.dto.ActivityResponse;
import com.luna.activity.entity.ActivityType;
import com.luna.activity.service.IActivityService;
import com.luna.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "User activity tracking and history")
@SecurityRequirement(name = "bearerAuth")
public class ActivityController {
    
    private final IActivityService activityService;
    
    @GetMapping("/me")
    @Operation(summary = "Get current user's activities")
    public ResponseEntity<Page<ActivityResponse>> getMyActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityResponse> activities = activityService.getUserActivities(userId, pageable);
        return ResponseEntity.ok(activities);
    }
    
    @GetMapping("/me/type/{activityType}")
    @Operation(summary = "Get current user's activities by type")
    public ResponseEntity<Page<ActivityResponse>> getMyActivitiesByType(
            @PathVariable ActivityType activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityResponse> activities = activityService
            .getUserActivitiesByType(userId, activityType, pageable);
        return ResponseEntity.ok(activities);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get activities for a specific user (activities on their content)")
    public ResponseEntity<Page<ActivityResponse>> getActivitiesForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityResponse> activities = activityService.getActivitiesForUser(userId, pageable);
        return ResponseEntity.ok(activities);
    }
}
