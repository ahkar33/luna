package com.luna.comment.service;

import com.luna.comment.dto.CommentResponse;
import com.luna.comment.dto.CreateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ICommentService {

    CommentResponse createComment(UUID postId, UUID userId, CreateCommentRequest request);

    CommentResponse updateComment(UUID commentId, UUID userId, String content);

    void deleteComment(UUID commentId, UUID userId);

    Page<CommentResponse> getPostComments(UUID postId, Pageable pageable);

    CommentResponse getComment(UUID commentId);

    long getCommentCount(UUID postId);
}
