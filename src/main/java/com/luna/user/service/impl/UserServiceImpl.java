package com.luna.user.service.impl;

import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.service.CloudinaryService;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.entity.User;
import com.luna.user.repository.UserRepository;
import com.luna.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    
    @Override
    @Transactional
    public UserProfileResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Delete old profile image if exists
        if (user.getProfileImageUrl() != null) {
            String oldPublicId = cloudinaryService.extractPublicId(user.getProfileImageUrl());
            if (oldPublicId != null) {
                cloudinaryService.deleteFile(oldPublicId);
            }
        }
        
        // Upload new profile image
        String imageUrl = cloudinaryService.uploadImage(image, "profiles");
        user.setProfileImageUrl(imageUrl);
        user = userRepository.save(user);
        
        return mapToUserProfileResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return mapToUserProfileResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return mapToUserProfileResponse(user);
    }
    
    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .profileImageUrl(user.getProfileImageUrl())
            .emailVerified(user.getEmailVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
