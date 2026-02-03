package com.luna.user.controller;

import com.luna.auth.dto.MessageResponse;
import com.luna.common.dto.PagedResponse;
import com.luna.security.SecurityUtils;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.service.IFollowService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Follow", description = "User follow/unfollow operations")
@SecurityRequirement(name = "bearerAuth")
public class FollowController {
    
    private final IFollowService followService;
    
    @PostMapping("/{userId}/follow")
    @Operation(summary = "Follow a user")
    public ResponseEntity<MessageResponse> followUser(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = SecurityUtils.getUserId(authentication);
        followService.followUser(currentUserId, userId);
        return ResponseEntity.ok(new MessageResponse("Successfully followed user"));
    }
    
    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<MessageResponse> unfollowUser(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = SecurityUtils.getUserId(authentication);
        followService.unfollowUser(currentUserId, userId);
        return ResponseEntity.ok(new MessageResponse("Successfully unfollowed user"));
    }
    
    @GetMapping("/{userId}/followers/count")
    @Operation(summary = "Get follower count")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowerCount(userId));
    }
    
    @GetMapping("/{userId}/following/count")
    @Operation(summary = "Get following count")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowingCount(userId));
    }
    
    @GetMapping("/{userId}/is-following")
    @Operation(summary = "Check if current user is following another user")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = SecurityUtils.getUserId(authentication);
        return ResponseEntity.ok(followService.isFollowing(currentUserId, userId));
    }
    
    @GetMapping("/{userId}/is-mutual")
    @Operation(summary = "Check if both users follow each other")
    public ResponseEntity<Boolean> isMutualFollow(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = SecurityUtils.getUserId(authentication);
        return ResponseEntity.ok(followService.isMutualFollow(currentUserId, userId));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get list of followers",
               description = "Returns paginated list of users who follow the specified user")
    public ResponseEntity<PagedResponse<UserProfileResponse>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> followers = followService.getFollowers(userId, pageable);
        return ResponseEntity.ok(PagedResponse.of(followers));
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "Get list of following",
               description = "Returns paginated list of users that the specified user follows")
    public ResponseEntity<PagedResponse<UserProfileResponse>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> following = followService.getFollowing(userId, pageable);
        return ResponseEntity.ok(PagedResponse.of(following));
    }

    @GetMapping("/{userId}/mutual-friends")
    @Operation(summary = "Get mutual friends",
               description = "Returns paginated list of users who both follow each other with the specified user")
    public ResponseEntity<PagedResponse<UserProfileResponse>> getMutualFriends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> mutualFriends = followService.getMutualFriends(userId, pageable);
        return ResponseEntity.ok(PagedResponse.of(mutualFriends));
    }
}
