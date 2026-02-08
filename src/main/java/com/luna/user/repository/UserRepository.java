package com.luna.user.repository;

import com.luna.auth.dto.AuthProvider;
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
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
    
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
    @Query(value = """
        WITH friend_suggestions AS (
            SELECT DISTINCT u.id, u.bio, u.country, u.country_code, u.created_at,
                   u.email, u.email_verified, u.is_active, u.password,
                   u.profile_image_url, u.role, u.updated_at, u.username,
                   u.auth_provider, u.provider_id,
                   (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = u.id) as follower_count
            FROM users u
            JOIN user_follows uf ON uf.following_id = u.id
            WHERE uf.follower_id IN (
                SELECT uf2.following_id
                FROM user_follows uf2
                WHERE uf2.follower_id = :userId
            )
            AND u.id != :userId
            AND u.is_active = true
            AND u.email_verified = true
            AND u.id NOT IN (
                SELECT uf3.following_id
                FROM user_follows uf3
                WHERE uf3.follower_id = :userId
            )
        )
        SELECT id, bio, country, country_code, created_at, email, email_verified,
               is_active, password, profile_image_url, role, updated_at, username,
               auth_provider, provider_id
        FROM friend_suggestions
        ORDER BY follower_count DESC
        LIMIT :#{#pageable.pageSize}
        """, nativeQuery = true)
    List<User> findFriendsOfFriends(@Param("userId") Long userId, Pageable pageable);
}
