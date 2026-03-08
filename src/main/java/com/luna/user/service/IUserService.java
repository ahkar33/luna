package com.luna.user.service;

import com.luna.user.dto.UpdateProfileRequest;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IUserService extends UserDetailsService {

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request, MultipartFile image);

    UserProfileResponse getUserProfile(UUID userId, UUID currentUserId);

    UserProfileResponse getUserProfileByUsername(String username);

    List<UserSuggestionResponse> getSuggestedUsers(UUID userId, int limit);

    Page<UserProfileResponse> searchUsers(String query, Pageable pageable);
}
