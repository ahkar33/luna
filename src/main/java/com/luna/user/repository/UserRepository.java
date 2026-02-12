package com.luna.user.repository;

import com.luna.auth.dto.AuthProvider;
import com.luna.user.dto.UserSuggestionProjection;
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
    
    // Graph-based suggestions: 2nd-degree connections with mutual count and follower count computed inline
    @Query(value = """
        SELECT u.id AS id,
               u.username AS username,
               u.profile_image_url AS profileImageUrl,
               u.country_code AS countryCode,
               COUNT(DISTINCT uf.follower_id) AS mutualConnectionCount,
               (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = u.id) AS followerCount
        FROM users u
        JOIN user_follows uf ON uf.following_id = u.id
            AND uf.follower_id IN (SELECT uf2.following_id FROM user_follows uf2 WHERE uf2.follower_id = :userId)
        WHERE u.id != :userId
          AND u.is_active = true
          AND u.email_verified = true
          AND u.id NOT IN (SELECT uf3.following_id FROM user_follows uf3 WHERE uf3.follower_id = :userId)
        GROUP BY u.id, u.username, u.profile_image_url, u.country_code
        ORDER BY mutualConnectionCount DESC, followerCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<UserSuggestionProjection> findGraphBasedSuggestions(@Param("userId") Long userId, @Param("limit") int limit);

    // Same-country users not already followed and not in excluded set
    @Query(value = """
        SELECT u.id AS id,
               u.username AS username,
               u.profile_image_url AS profileImageUrl,
               u.country_code AS countryCode,
               0 AS mutualConnectionCount,
               (SELECT COUNT(*) FROM user_follows f WHERE f.following_id = u.id) AS followerCount
        FROM users u
        WHERE u.country_code = :countryCode
          AND u.id != :userId
          AND u.is_active = true
          AND u.email_verified = true
          AND u.id NOT IN (SELECT uf.following_id FROM user_follows uf WHERE uf.follower_id = :userId)
          AND u.id NOT IN (:excludeIds)
        ORDER BY followerCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<UserSuggestionProjection> findSameCountryUsersExcluding(
            @Param("userId") Long userId,
            @Param("countryCode") String countryCode,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("limit") int limit);
}
