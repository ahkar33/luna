package com.luna.user.controller;

import com.luna.common.dto.ApiResponse;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Tag(name = "Internal User API", description = "Internal endpoints for service-to-service communication")
public class InternalUserController {

    private final IUserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID (Internal)", description = "Fetch user details for internal services")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long userId) {
        UserProfileResponse user = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username (Internal)", description = "Fetch user details by username for internal services")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByUsername(@PathVariable String username) {
        UserProfileResponse user = userService.getUserProfileByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }
}
