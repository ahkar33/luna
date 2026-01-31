package com.luna.post.repository;

import com.luna.post.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    
    Optional<Hashtag> findByName(String name);
    
    // Search hashtags by prefix
    List<Hashtag> findByNameStartingWithOrderByNameAsc(String prefix, Pageable pageable);
    
    // Get trending hashtags (most used in recent posts)
    @Query("""
        SELECT h FROM Hashtag h
        JOIN PostHashtag ph ON ph.hashtag.id = h.id
        JOIN Post p ON ph.post.id = p.id
        WHERE p.createdAt > :since AND p.deletedAt IS NULL
        GROUP BY h.id
        ORDER BY COUNT(ph.id) DESC
        """)
    List<Hashtag> findTrendingHashtags(@Param("since") LocalDateTime since, Pageable pageable);
}
