package com.luna.comment.controller;

import com.luna.auth.dto.MessageResponse;
import com.luna.comment.dto.CommentResponse;
import com.luna.comment.dto.CreateCommentRequest;
import com.luna.comment.service.ICommentService;
import com.luna.common.dto.PagedResponse;
import com.luna.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        CommentResponse response = commentService.createComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments for a post",
               description = "Returns paginated top-level comments with nested replies")
    public ResponseEntity<PagedResponse<CommentResponse>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<CommentResponse> comments = commentService.getPostComments(postId, pageable);
        return ResponseEntity.ok(PagedResponse.of(comments));
    }

    @GetMapping("/comments/{commentId}")
    @Operation(summary = "Get a single comment with its replies")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long commentId) {
        CommentResponse response = commentService.getComment(commentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Update a comment", description = "Only the comment author can update")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        CommentResponse response = commentService.updateComment(commentId, userId, request.getContent());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment", description = "Only the comment author can delete. Deletes all replies too.")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(new MessageResponse("Comment deleted successfully"));
    }

    @GetMapping("/posts/{postId}/comments/count")
    @Operation(summary = "Get total comment count for a post")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long postId) {
        long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }
}
