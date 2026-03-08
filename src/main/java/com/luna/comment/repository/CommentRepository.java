package com.luna.comment.repository;

import com.luna.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // Get top-level comments for a post
    Page<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    // Count top-level comments for a post
    long countByPostIdAndParentIsNull(UUID postId);

    // Count all comments for a post
    long countByPostId(UUID postId);

    // Count replies for a comment
    long countByParentId(UUID parentId);

    // Check if user owns the comment
    boolean existsByIdAndAuthorId(UUID commentId, UUID userId);
}
