package com.luna.user.repository;

import com.luna.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
    // Search users by username (case-insensitive, partial match)
    @Query("""
        SELECT u FROM User u 
        WHERE u.isActive = true AND u.emailVerified = true
        AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY 
            CASE WHEN LOWER(u.username) = LOWER(:query) THEN 0
                 WHEN LOWER(u.username) LIKE LOWER(CONCAT(:query, '%')) THEN 1
                 ELSE 2 END,
            (SELECT COUNT(f) FROM UserFollow f WHERE f.following.id = u.id) DESC
        """)
    Page<User> searchByUsername(@Param("query") String query, Pageable pageable);
    
    // Get popular users (most followers) excluding current user
    @Query("""
        SELECT u FROM User u 
        WHERE u.id != :userId AND u.isActive = true AND u.emailVerified = true
        AND u.id NOT IN (SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId)
        ORDER BY (SELECT COUNT(f) FROM UserFollow f WHERE f.following.id = u.id) DESC
        """)
    List<User> findPopularUsersExcluding(@Param("userId") Long userId, Pageable pageable);
    
    // Get friends of friends (users followed by people the current user follows)
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN UserFollow uf ON uf.following.id = u.id
        WHERE uf.follower.id IN (SELECT uf2.following.id FROM UserFollow uf2 WHERE uf2.follower.id = :userId)
        AND u.id != :userId
        AND u.isActive = true
        AND u.emailVerified = true
        AND u.id NOT IN (SELECT uf3.following.id FROM UserFollow uf3 WHERE uf3.follower.id = :userId)
        ORDER BY (SELECT COUNT(f) FROM UserFollow f WHERE f.following.id = u.id) DESC
        """)
    List<User> findFriendsOfFriends(@Param("userId") Long userId, Pageable pageable);
}
