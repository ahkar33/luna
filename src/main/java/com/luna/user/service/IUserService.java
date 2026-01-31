package com.luna.user.service;

import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUserService extends UserDetailsService {
    
    UserProfileResponse updateProfileImage(Long userId, MultipartFile image);
    
    UserProfileResponse getUserProfile(Long userId);
    
    UserProfileResponse getUserProfileByUsername(String username);
    
    List<UserSuggestionResponse> getSuggestedUsers(Long userId, int limit);
}
