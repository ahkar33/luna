package com.luna.user.repository;

import com.luna.user.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    long countByFollowerId(Long followerId);
    
    long countByFollowingId(Long followingId);
}
