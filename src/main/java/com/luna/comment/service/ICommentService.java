package com.luna.comment.service;

import com.luna.comment.dto.CommentResponse;
import com.luna.comment.dto.CreateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICommentService {
    
    CommentResponse createComment(Long postId, Long userId, CreateCommentRequest request);
    
    CommentResponse updateComment(Long commentId, Long userId, String content);
    
    void deleteComment(Long commentId, Long userId);
    
    Page<CommentResponse> getPostComments(Long postId, Pageable pageable);
    
    CommentResponse getComment(Long commentId);
    
    long getCommentCount(Long postId);
}
