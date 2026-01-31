package com.luna.post.repository;

import com.luna.post.entity.Repost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepostRepository extends JpaRepository<Repost, Long> {
    
    boolean existsByUserIdAndOriginalPostId(Long userId, Long postId);
    
    Optional<Repost> findByUserIdAndOriginalPostId(Long userId, Long postId);
    
    Page<Repost> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    void deleteByUserIdAndOriginalPostId(Long userId, Long postId);
    
    long countByOriginalPostId(Long postId);
}
