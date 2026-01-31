package com.luna.user.service.impl;

import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.service.CloudinaryService;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionResponse;
import com.luna.user.entity.User;
import com.luna.user.repository.UserFollowRepository;
import com.luna.user.repository.UserRepository;
import com.luna.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
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
            .countryCode(user.getCountryCode())
            .country(user.getCountry())
            .emailVerified(user.getEmailVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserSuggestionResponse> getSuggestedUsers(Long userId, int limit) {
        List<UserSuggestionResponse> suggestions = new ArrayList<>();
        Set<Long> addedUserIds = new HashSet<>();
        
        // Check if user follows anyone
        long followingCount = userFollowRepository.countByFollowerId(userId);
        
        if (followingCount > 0) {
            // User follows people - get friends of friends first
            List<User> friendsOfFriends = userRepository.findFriendsOfFriends(userId, PageRequest.of(0, limit));
            
            for (User user : friendsOfFriends) {
                if (addedUserIds.add(user.getId())) {
                    int mutualCount = userFollowRepository.countMutualConnections(userId, user.getId());
                    long followerCount = userFollowRepository.countByFollowingId(user.getId());
                    
                    suggestions.add(UserSuggestionResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .profileImageUrl(user.getProfileImageUrl())
                        .followerCount(followerCount)
                        .mutualConnections(mutualCount)
                        .suggestionReason(mutualCount > 0 
                            ? "Followed by " + mutualCount + " people you follow" 
                            : "You might know")
                        .build());
                }
            }
        }
        
        // Fill remaining slots with popular users (cold start or not enough friends of friends)
        if (suggestions.size() < limit) {
            int remaining = limit - suggestions.size();
            List<User> popularUsers = userRepository.findPopularUsersExcluding(userId, PageRequest.of(0, remaining + addedUserIds.size()));
            
            for (User user : popularUsers) {
                if (suggestions.size() >= limit) break;
                
                if (addedUserIds.add(user.getId())) {
                    long followerCount = userFollowRepository.countByFollowingId(user.getId());
                    
                    suggestions.add(UserSuggestionResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .profileImageUrl(user.getProfileImageUrl())
                        .followerCount(followerCount)
                        .mutualConnections(0)
                        .suggestionReason("Popular on Luna")
                        .build());
                }
            }
        }
        
        return suggestions;
    }
}
