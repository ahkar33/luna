package com.luna.user.service;

import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUserService extends UserDetailsService {
    
    UserProfileResponse updateProfileImage(Long userId, MultipartFile image);
    
    UserProfileResponse updateBio(Long userId, String bio);
    
    UserProfileResponse getUserProfile(Long userId, Long currentUserId);
    
    UserProfileResponse getUserProfileByUsername(String username);
    
    List<UserSuggestionResponse> getSuggestedUsers(Long userId, int limit);
    
    Page<UserProfileResponse> searchUsers(String query, Pageable pageable);
}
