package com.luna.user.service;

import com.luna.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IFollowService {

    void followUser(UUID followerId, UUID followingId);

    void unfollowUser(UUID followerId, UUID followingId);

    boolean isFollowing(UUID followerId, UUID followingId);

    boolean isMutualFollow(UUID userId1, UUID userId2);

    long getFollowerCount(UUID userId);

    long getFollowingCount(UUID userId);

    Page<UserProfileResponse> getFollowers(UUID userId, Pageable pageable);

    Page<UserProfileResponse> getFollowing(UUID userId, Pageable pageable);

    Page<UserProfileResponse> getMutualFriends(UUID userId, Pageable pageable);
}
