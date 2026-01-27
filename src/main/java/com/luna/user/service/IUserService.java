package com.luna.user.service;

import com.luna.user.dto.UserProfileResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService extends UserDetailsService {
    
    UserProfileResponse updateProfileImage(Long userId, MultipartFile image);
    
    UserProfileResponse getUserProfile(Long userId);
}
