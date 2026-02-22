package com.luna.user.controller;

import com.luna.common.dto.PagedResponse;
import com.luna.security.SecurityUtils;
import com.luna.user.dto.UpdateProfileRequest;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionResponse;
import com.luna.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    
    private final IUserService userService;
    
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        UserProfileResponse response = userService.getUserProfile(userId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable("userId") Long userId,
            Authentication authentication) {
        Long currentUserId = authentication != null ? SecurityUtils.getUserId(authentication) : null;
        UserProfileResponse response = userService.getUserProfile(userId, currentUserId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update profile",
               description = "Update display name, bio, and/or profile image. All fields are optional.")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "username", required = false) String username,
            @RequestPart(value = "displayName", required = false) String displayName,
            @RequestPart(value = "bio", required = false) String bio,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername(username);
        request.setDisplayName(displayName);
        request.setBio(bio);
        UserProfileResponse response = userService.updateProfile(userId, request, image);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/suggestions")
    @Operation(summary = "Get suggested users to follow",
               description = "Returns user suggestions based on mutual connections or popularity for new users")
    public ResponseEntity<List<UserSuggestionResponse>> getSuggestedUsers(
            @Parameter(description = "Number of suggestions to return (max: 50)", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        List<UserSuggestionResponse> suggestions = userService.getSuggestedUsers(userId, Math.min(limit, 50));
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username",
               description = "Search for users by username. Results are sorted by relevance and popularity.")
    public ResponseEntity<PagedResponse<UserProfileResponse>> searchUsers(
            @Parameter(description = "Search query (username)", example = "john")
            @RequestParam(name = "q") String q,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (max: 50)", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> results = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(PagedResponse.of(results));
    }
}
