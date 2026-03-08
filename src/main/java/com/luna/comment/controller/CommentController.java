package com.luna.comment.controller;

import com.luna.comment.dto.CommentResponse;
import com.luna.comment.dto.CreateCommentRequest;
import com.luna.comment.service.ICommentService;
import com.luna.common.dto.ApiResponse;
import com.luna.common.dto.PagedResponse;
import com.luna.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment management operations")
@SecurityRequirement(name = "bearerAuth")
public class CommentController {

    private final ICommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Create a comment on a post",
               description = "Create a top-level comment or reply to an existing comment. Max nesting depth is 3 levels.")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable("postId") UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        CommentResponse response = commentService.createComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments for a post",
               description = "Returns paginated top-level comments with nested replies")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getPostComments(
            @PathVariable("postId") UUID postId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of comments per page") @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<CommentResponse> comments = commentService.getPostComments(postId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(comments)));
    }

    @GetMapping("/comments/{commentId}")
    @Operation(summary = "Get a single comment with its replies")
    public ResponseEntity<ApiResponse<CommentResponse>> getComment(@PathVariable("commentId") UUID commentId) {
        CommentResponse response = commentService.getComment(commentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update a comment", description = "Only the comment author can update")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable("commentId") UUID commentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        CommentResponse response = commentService.updateComment(commentId, userId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment", description = "Only the comment author can delete. Deletes all replies too.")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("commentId") UUID commentId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully"));
    }

    @GetMapping("/posts/{postId}/comments/count")
    @Operation(summary = "Get total comment count for a post")
    public ResponseEntity<ApiResponse<Long>> getCommentCount(@PathVariable("postId") UUID postId) {
        long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
