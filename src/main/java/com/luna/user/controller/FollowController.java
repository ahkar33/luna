package com.luna.user.controller;

import com.luna.auth.dto.MessageResponse;
import com.luna.user.service.IFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
        Long currentUserId = Long.parseLong(authentication.getName());
        followService.followUser(currentUserId, userId);
        return ResponseEntity.ok(new MessageResponse("Successfully followed user"));
    }
    
    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<MessageResponse> unfollowUser(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getName());
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
        Long currentUserId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(followService.isFollowing(currentUserId, userId));
    }
    
    @GetMapping("/{userId}/is-mutual")
    @Operation(summary = "Check if both users follow each other")
    public ResponseEntity<Boolean> isMutualFollow(
            @PathVariable Long userId,
            Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(followService.isMutualFollow(currentUserId, userId));
    }
}
