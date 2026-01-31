package com.luna.post.repository;

import com.luna.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(@Param("authorId") Long authorId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.author.id IN " +
           "(SELECT f.following.id FROM UserFollow f WHERE f.follower.id = :userId) " +
           "AND p.deletedAt IS NULL " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findTimelinePosts(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NOT NULL AND p.deletedAt < :cutoffDate")
    List<Post> findPostsToHardDelete(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("""
        SELECT DISTINCT p FROM Post p
        JOIN PostHashtag ph ON ph.post.id = p.id
        JOIN Hashtag h ON ph.hashtag.id = h.id
        WHERE h.name = :hashtagName AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
        """)
    Page<Post> findByHashtag(@Param("hashtagName") String hashtagName, Pageable pageable);
}
