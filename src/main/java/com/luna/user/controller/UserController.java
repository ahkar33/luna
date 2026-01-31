package com.luna.user.controller;

import com.luna.user.dto.UpdateBioRequest;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionResponse;
import com.luna.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
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

import jakarta.validation.Valid;
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
        Long userId = Long.parseLong(authentication.getName());
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{userId}/profile")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update profile image")
    public ResponseEntity<UserProfileResponse> updateProfileImage(
            @RequestPart MultipartFile image,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserProfileResponse response = userService.updateProfileImage(userId, image);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/profile/bio")
    @Operation(summary = "Update profile bio", description = "Update user bio (max 500 characters)")
    public ResponseEntity<UserProfileResponse> updateBio(
            @Valid @RequestBody UpdateBioRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserProfileResponse response = userService.updateBio(userId, request.getBio());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/suggestions")
    @Operation(summary = "Get suggested users to follow", 
               description = "Returns user suggestions based on mutual connections or popularity for new users")
    public ResponseEntity<List<UserSuggestionResponse>> getSuggestedUsers(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<UserSuggestionResponse> suggestions = userService.getSuggestedUsers(userId, Math.min(limit, 50));
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username", 
               description = "Search for users by username. Results are sorted by relevance and popularity.")
    public ResponseEntity<Page<UserProfileResponse>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<UserProfileResponse> results = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(results);
    }
}
