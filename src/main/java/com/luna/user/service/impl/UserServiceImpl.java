package com.luna.user.service.impl;

import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.service.CloudinaryService;
import com.luna.post.repository.PostRepository;
import com.luna.user.dto.UpdateProfileRequest;
import com.luna.user.dto.UserProfileResponse;
import com.luna.user.dto.UserSuggestionProjection;
import com.luna.user.dto.UserSuggestionResponse;
import com.luna.user.entity.User;
import com.luna.user.repository.UserFollowRepository;
import com.luna.user.repository.UserRepository;
import com.luna.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final CloudinaryService cloudinaryService;
    private final PostRepository postRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by email first (new behavior)
        return userRepository.findByEmail(username)
                // Fall back to finding by username field for backward compatibility with old tokens
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    
    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (image != null && !image.isEmpty()) {
            // Delete old profile image from Cloudinary before uploading new one
            if (user.getProfileImageUrl() != null) {
                String oldPublicId = cloudinaryService.extractPublicId(user.getProfileImageUrl());
                if (oldPublicId != null) {
                    cloudinaryService.deleteFile(oldPublicId);
                }
            }
            String imageUrl = cloudinaryService.uploadImage(image, "profiles");
            user.setProfileImageUrl(imageUrl);
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);
        return mapToUserProfileResponse(user, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserProfileResponse(user, currentUserId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return mapToUserProfileResponse(user);
    }
    
    private UserProfileResponse mapToUserProfileResponse(User user) {
        return mapToUserProfileResponse(user, null);
    }

    private UserProfileResponse mapToUserProfileResponse(User user, Long currentUserId) {
        long followerCount = userFollowRepository.countByFollowingId(user.getId());
        long followingCount = userFollowRepository.countByFollowerId(user.getId());
        long postCount = postRepository.countByAuthorId(user.getId());

        return UserProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsernameField())
            .displayName(user.getDisplayName())
            .email(user.getEmail())
            .profileImageUrl(user.getProfileImageUrl())
            .bio(user.getBio())
            .countryCode(user.getCountryCode())
            .country(user.getCountry())
            .emailVerified(user.getEmailVerified())
            .createdAt(user.getCreatedAt())
            .followerCount(followerCount)
            .followingCount(followingCount)
            .postCount(postCount)
            .isMyProfile(user.getId().equals(currentUserId))
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserSuggestionResponse> getSuggestedUsers(Long userId, int limit) {
        List<UserSuggestionResponse> suggestions = new ArrayList<>();
        Set<Long> addedUserIds = new HashSet<>();

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String userCountryCode = currentUser.getCountryCode();

        long followingCount = userFollowRepository.countByFollowerId(userId);

        // TIER 1: Graph-based (2nd-degree connections with composite scoring)
        if (followingCount > 0) {
            List<UserSuggestionProjection> graphResults =
                    userRepository.findGraphBasedSuggestions(userId, limit * 2);

            // Apply composite scoring
            List<UserSuggestionProjection> scored = graphResults.stream()
                    .sorted((a, b) -> Double.compare(
                            computeScore(b, userCountryCode),
                            computeScore(a, userCountryCode)))
                    .limit(limit)
                    .toList();

            // Batch-fetch mutual connection usernames for all scored suggestions
            List<Long> scoredIds = scored.stream().map(UserSuggestionProjection::getId).toList();
            Map<Long, List<String>> mutualUsernamesMap = new HashMap<>();
            if (!scoredIds.isEmpty()) {
                List<Object[]> mutualRows = userFollowRepository.findMutualConnectionUsernames(userId, scoredIds);
                for (Object[] row : mutualRows) {
                    Long suggestedId = ((Number) row[0]).longValue();
                    String mutualUsername = (String) row[1];
                    mutualUsernamesMap.computeIfAbsent(suggestedId, k -> new ArrayList<>()).add(mutualUsername);
                }
            }

            for (UserSuggestionProjection p : scored) {
                if (addedUserIds.add(p.getId())) {
                    List<String> mutualUsernames = mutualUsernamesMap.getOrDefault(p.getId(), List.of());
                    int mutualCount = p.getMutualConnectionCount();

                    suggestions.add(UserSuggestionResponse.builder()
                            .id(p.getId())
                            .username(p.getUsername())
                            .displayName(p.getDisplayName())
                            .profileImageUrl(p.getProfileImageUrl())
                            .followerCount(p.getFollowerCount())
                            .mutualConnections(mutualCount)
                            .mutualConnectionUsernames(mutualUsernames)
                            .suggestionReason(buildMutualReason(mutualUsernames, mutualCount))
                            .isFollowing(false)
                            .build());
                }
            }
        }

        // TIER 2: Same country (geographic fallback)
        if (suggestions.size() < limit && userCountryCode != null && !userCountryCode.isBlank()) {
            int remaining = limit - suggestions.size();
            List<Long> excludeIds = new ArrayList<>(addedUserIds);
            if (excludeIds.isEmpty()) {
                excludeIds.add(0L); // placeholder to avoid empty IN clause
            }

            List<UserSuggestionProjection> countryResults =
                    userRepository.findSameCountryUsersExcluding(userId, userCountryCode, excludeIds, remaining);

            for (UserSuggestionProjection p : countryResults) {
                if (suggestions.size() >= limit) break;
                if (addedUserIds.add(p.getId())) {
                    suggestions.add(UserSuggestionResponse.builder()
                            .id(p.getId())
                            .username(p.getUsername())
                            .displayName(p.getDisplayName())
                            .profileImageUrl(p.getProfileImageUrl())
                            .followerCount(p.getFollowerCount())
                            .mutualConnections(0)
                            .mutualConnectionUsernames(List.of())
                            .suggestionReason("From your country")
                            .isFollowing(false)
                            .build());
                }
            }
        }

        // TIER 3: Popular users (cold start fallback)
        if (suggestions.size() < limit) {
            int remaining = limit - suggestions.size();
            List<User> popularUsers = userRepository.findPopularUsersExcluding(
                    userId, PageRequest.of(0, remaining + addedUserIds.size()));

            for (User user : popularUsers) {
                if (suggestions.size() >= limit) break;
                if (addedUserIds.add(user.getId())) {
                    suggestions.add(UserSuggestionResponse.builder()
                            .id(user.getId())
                            .username(user.getUsernameField())
                            .displayName(user.getDisplayName())
                            .profileImageUrl(user.getProfileImageUrl())
                            .followerCount(userFollowRepository.countByFollowingId(user.getId()))
                            .mutualConnections(0)
                            .mutualConnectionUsernames(List.of())
                            .suggestionReason("Popular on Luna")
                            .isFollowing(false)
                            .build());
                }
            }
        }

        return suggestions;
    }

    private double computeScore(UserSuggestionProjection projection, String userCountryCode) {
        double score = projection.getMutualConnectionCount() * 10.0;
        if (userCountryCode != null && userCountryCode.equals(projection.getCountryCode())) {
            score += 5.0;
        }
        score += Math.min(Math.log(projection.getFollowerCount() + 1) / Math.log(2), 10.0);
        return score;
    }

    private String buildMutualReason(List<String> mutualUsernames, int mutualCount) {
        if (mutualUsernames.isEmpty()) {
            return "You might know";
        }
        if (mutualUsernames.size() == 1 && mutualCount == 1) {
            return "Followed by " + mutualUsernames.get(0);
        }
        if (mutualUsernames.size() >= 2 && mutualCount == 2) {
            return "Followed by " + mutualUsernames.get(0) + " and " + mutualUsernames.get(1);
        }
        // Show up to 2 names + "and N others"
        int others = mutualCount - 2;
        return "Followed by " + mutualUsernames.get(0) + ", " + mutualUsernames.get(1)
                + ", and " + others + (others == 1 ? " other" : " others");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchUsers(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return Page.empty(pageable);
        }
        
        Page<User> users = userRepository.searchByUsername(query.trim(), pageable);
        return users.map(this::mapToUserProfileResponse);
    }
}
