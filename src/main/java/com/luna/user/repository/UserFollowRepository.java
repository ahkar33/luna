package com.luna.user.repository;

import com.luna.user.entity.User;
import com.luna.user.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Get list of followers (users who follow the specified user)
    @Query("""
        SELECT uf.follower FROM UserFollow uf
        WHERE uf.following.id = :userId
        ORDER BY uf.createdAt DESC
        """)
    Page<User> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get list of following (users that the specified user follows)
    @Query("""
        SELECT uf.following FROM UserFollow uf
        WHERE uf.follower.id = :userId
        ORDER BY uf.createdAt DESC
        """)
    Page<User> findFollowingByUserId(@Param("userId") Long userId, Pageable pageable);

    // Get mutual friends (users who follow each other)
    @Query(value = """
        SELECT uf1.following FROM UserFollow uf1
        WHERE uf1.follower.id = :userId
        AND EXISTS (
            SELECT 1 FROM UserFollow uf2
            WHERE uf2.follower.id = uf1.following.id
            AND uf2.following.id = :userId
        )
        ORDER BY uf1.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(uf1) FROM UserFollow uf1
        WHERE uf1.follower.id = :userId
        AND EXISTS (
            SELECT 1 FROM UserFollow uf2
            WHERE uf2.follower.id = uf1.following.id
            AND uf2.following.id = :userId
        )
        """)
    Page<User> findMutualFriends(@Param("userId") Long userId, Pageable pageable);
}
