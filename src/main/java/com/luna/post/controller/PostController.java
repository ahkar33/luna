package com.luna.post.controller;

import com.luna.auth.dto.MessageResponse;
import com.luna.post.dto.CreatePostRequest;
import com.luna.post.dto.PostResponse;
import com.luna.post.service.IPostService;
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
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post management operations")
@SecurityRequirement(name = "bearerAuth")
public class PostController {
    
    private final IPostService postService;
    
    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PostResponse response = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PostResponse response = postService.getPostById(postId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user")
    public ResponseEntity<Page<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getUserPosts(userId, currentUserId, pageable);
        return ResponseEntity.ok(posts);
    }
    
    @GetMapping("/timeline")
    @Operation(summary = "Get timeline posts from followed users")
    public ResponseEntity<Page<PostResponse>> getTimelinePosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getTimelinePosts(userId, pageable);
        return ResponseEntity.ok(posts);
    }
    
    @DeleteMapping("/{postId}")
    @Operation(summary = "Soft delete a post (can be restored within 30 days)")
    public ResponseEntity<MessageResponse> deletePost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(new MessageResponse("Post deleted successfully. You can restore it within 30 days."));
    }
    
    @PostMapping("/{postId}/restore")
    @Operation(summary = "Restore a soft-deleted post")
    public ResponseEntity<MessageResponse> restorePost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        postService.restorePost(postId, userId);
        return ResponseEntity.ok(new MessageResponse("Post restored successfully"));
    }
    
    @PostMapping("/{postId}/like")
    @Operation(summary = "Like a post")
    public ResponseEntity<PostResponse> likePost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PostResponse response = postService.likePost(postId, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{postId}/like")
    @Operation(summary = "Unlike a post")
    public ResponseEntity<PostResponse> unlikePost(
            @PathVariable Long postId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        PostResponse response = postService.unlikePost(postId, userId);
        return ResponseEntity.ok(response);
    }
}
