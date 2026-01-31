package com.luna.post.repository;

import com.luna.post.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {
    
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    Optional<SavedPost> findByUserIdAndPostId(Long userId, Long postId);
    
    Page<SavedPost> findByUserIdOrderBySavedAtDesc(Long userId, Pageable pageable);
    
    void deleteByUserIdAndPostId(Long userId, Long postId);
}
