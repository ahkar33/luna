package com.luna.user.controller;

import com.luna.user.dto.UserProfileResponse;
import com.luna.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
