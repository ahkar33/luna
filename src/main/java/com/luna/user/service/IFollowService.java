package com.luna.user.service;

public interface IFollowService {
    
    void followUser(Long followerId, Long followingId);
    
    void unfollowUser(Long followerId, Long followingId);
    
    boolean isFollowing(Long followerId, Long followingId);
    
    boolean isMutualFollow(Long userId1, Long userId2);
    
    long getFollowerCount(Long userId);
    
    long getFollowingCount(Long userId);
}
