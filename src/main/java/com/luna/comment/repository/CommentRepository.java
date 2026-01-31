package com.luna.comment.repository;

import com.luna.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Get top-level comments for a post
    Page<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(Long postId, Pageable pageable);
    
    // Count top-level comments for a post
    long countByPostIdAndParentIsNull(Long postId);
    
    // Count all comments for a post
    long countByPostId(Long postId);
    
    // Count replies for a comment
    long countByParentId(Long parentId);
    
    // Check if user owns the comment
    boolean existsByIdAndAuthorId(Long commentId, Long userId);
}
