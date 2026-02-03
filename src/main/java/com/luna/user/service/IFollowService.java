package com.luna.user.service;

import com.luna.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IFollowService {

    void followUser(Long followerId, Long followingId);

    void unfollowUser(Long followerId, Long followingId);

    boolean isFollowing(Long followerId, Long followingId);

    boolean isMutualFollow(Long userId1, Long userId2);

    long getFollowerCount(Long userId);

    long getFollowingCount(Long userId);

    Page<UserProfileResponse> getFollowers(Long userId, Pageable pageable);

    Page<UserProfileResponse> getFollowing(Long userId, Pageable pageable);

    Page<UserProfileResponse> getMutualFriends(Long userId, Pageable pageable);
}
