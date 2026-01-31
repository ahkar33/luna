package com.luna.post.repository;

import com.luna.post.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {
    
    void deleteByPostId(Long postId);
    
    List<PostHashtag> findByPostId(Long postId);
    
    // Count posts using a hashtag (for trending calculation)
    @Query("""
        SELECT COUNT(DISTINCT ph.post.id) FROM PostHashtag ph
        JOIN Post p ON ph.post.id = p.id
        WHERE ph.hashtag.id = :hashtagId 
        AND p.deletedAt IS NULL
        AND p.createdAt > :since
        """)
    long countRecentPostsByHashtagId(@Param("hashtagId") Long hashtagId, @Param("since") LocalDateTime since);
}
