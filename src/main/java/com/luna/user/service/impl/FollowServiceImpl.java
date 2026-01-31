package com.luna.user.service.impl;

import com.luna.activity.entity.ActivityType;
import com.luna.activity.service.IActivityService;
import com.luna.common.exception.BadRequestException;
import com.luna.common.exception.ResourceNotFoundException;
import com.luna.user.entity.User;
import com.luna.user.entity.UserFollow;
import com.luna.user.repository.UserFollowRepository;
import com.luna.user.repository.UserRepository;
import com.luna.user.service.IFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {
    
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final IActivityService activityService;
    
    @Override
    @Transactional
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        
        if (userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BadRequestException("You are already following this user");
        }
        
        User follower = userRepository.findById(followerId)
            .orElseThrow(() -> new ResourceNotFoundException("Follower not found"));
        
        User following = userRepository.findById(followingId)
            .orElseThrow(() -> new ResourceNotFoundException("User to follow not found"));
        
        UserFollow userFollow = UserFollow.builder()
            .follower(follower)
            .following(following)
            .build();
        
        userFollowRepository.save(userFollow);
        
        // Log activity
        activityService.logActivity(followerId, ActivityType.FOLLOW, "USER", 
            followingId, followingId, null);
    }
    
    @Override
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        if (!userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BadRequestException("You are not following this user");
        }
        
        userFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        
        // Log activity
        activityService.logActivity(followerId, ActivityType.UNFOLLOW, "USER", 
            followingId, followingId, null);
    }
    
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }
    
    @Override
    public boolean isMutualFollow(Long userId1, Long userId2) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(userId1, userId2)
            && userFollowRepository.existsByFollowerIdAndFollowingId(userId2, userId1);
    }
    
    @Override
    public long getFollowerCount(Long userId) {
        return userFollowRepository.countByFollowingId(userId);
    }
    
    @Override
    public long getFollowingCount(Long userId) {
        return userFollowRepository.countByFollowerId(userId);
    }
}
