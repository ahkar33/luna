package com.luna.user.controller;

import com.luna.common.dto.ApiResponse;
import com.luna.common.dto.PagedResponse;
import com.luna.security.SecurityUtils;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.service.IFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Follow", description = "User follow/unfollow operations")
@SecurityRequirement(name = "bearerAuth")
public class FollowController {

    private final IFollowService followService;

    @PostMapping("/{userId}/follow")
    @Operation(summary = "Follow a user")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        followService.followUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully followed user"));
    }

    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        followService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed user"));
    }

    @GetMapping("/{userId}/followers/count")
    @Operation(summary = "Get follower count")
    public ResponseEntity<ApiResponse<Long>> getFollowerCount(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowerCount(userId)));
    }

    @GetMapping("/{userId}/following/count")
    @Operation(summary = "Get following count")
    public ResponseEntity<ApiResponse<Long>> getFollowingCount(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowingCount(userId)));
    }

    @GetMapping("/{userId}/is-following")
    @Operation(summary = "Check if current user is following another user")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(followService.isFollowing(currentUserId, userId)));
    }

    @GetMapping("/{userId}/is-mutual")
    @Operation(summary = "Check if both users follow each other")
    public ResponseEntity<ApiResponse<Boolean>> isMutualFollow(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(followService.isMutualFollow(currentUserId, userId)));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get list of followers",
               description = "Returns paginated list of users who follow the specified user")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getFollowers(
            @PathVariable("userId") UUID userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of followers per page") @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> followers = followService.getFollowers(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(followers)));
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "Get list of following",
               description = "Returns paginated list of users that the specified user follows")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getFollowing(
            @PathVariable("userId") UUID userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of users per page") @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> following = followService.getFollowing(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(following)));
    }

    @GetMapping("/mutual-friends")
    @Operation(summary = "Get current user's mutual friends",
               description = "Returns paginated list of users who both follow each other with the current user")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getCurrentUserMutualFriends(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of mutual friends per page") @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> mutualFriends = followService.getMutualFriends(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(mutualFriends)));
    }

    @GetMapping("/{userId}/mutual-friends")
    @Operation(summary = "Get mutual friends",
               description = "Returns paginated list of users who both follow each other with the specified user")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getMutualFriends(
            @PathVariable("userId") UUID userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of mutual friends per page") @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> mutualFriends = followService.getMutualFriends(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(mutualFriends)));
    }
}
