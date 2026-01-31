package com.luna.user.repository;

import com.luna.user.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    long countByFollowerId(Long followerId);
    
    long countByFollowingId(Long followingId);
    
    // Count mutual followers between current user's following and a suggested user
    @Query("""
        SELECT COUNT(DISTINCT uf.follower.id) FROM UserFollow uf
        WHERE uf.following.id = :suggestedUserId
        AND uf.follower.id IN (SELECT uf2.following.id FROM UserFollow uf2 WHERE uf2.follower.id = :currentUserId)
        """)
    int countMutualConnections(@Param("currentUserId") Long currentUserId, @Param("suggestedUserId") Long suggestedUserId);
}
