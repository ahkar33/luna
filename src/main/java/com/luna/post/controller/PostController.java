package com.luna.post.controller;

import com.luna.common.dto.ApiResponse;
import com.luna.common.dto.PagedResponse;
import com.luna.post.dto.CreatePostRequest;
import com.luna.post.dto.PostResponse;
import com.luna.post.dto.RepostRequest;
import com.luna.post.dto.RepostResponse;
import com.luna.post.service.IPostService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post management operations")
@SecurityRequirement(name = "bearerAuth")
public class PostController {
    
    private final IPostService postService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new post with optional images or videos",
               description = "Upload images OR videos (not both). Supported image types: JPEG, PNG, WEBP. Video types: MP4, WEBM, MOV.")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @ModelAttribute CreatePostRequest request,
            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @RequestPart(name = "videos", required = false) List<MultipartFile> videos,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        PostResponse response = postService.createPost(request, userId, images, videos);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        PostResponse response = postService.getPostById(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getUserPosts(
            @PathVariable("userId") UUID userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page") @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getUserPosts(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(posts)));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get timeline posts from followed users")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getTimelinePosts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page") @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getTimelinePosts(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(posts)));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Soft delete a post (can be restored within 30 days)")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully. You can restore it within 30 days."));
    }

    @PostMapping("/{postId}/restore")
    @Operation(summary = "Restore a soft-deleted post")
    public ResponseEntity<ApiResponse<Void>> restorePost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        postService.restorePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Post restored successfully"));
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "Like a post")
    public ResponseEntity<ApiResponse<PostResponse>> likePost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        PostResponse response = postService.likePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "Unlike a post")
    public ResponseEntity<ApiResponse<PostResponse>> unlikePost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        PostResponse response = postService.unlikePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{postId}/save")
    @Operation(summary = "Save a post for later")
    public ResponseEntity<ApiResponse<PostResponse>> savePost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        PostResponse response = postService.savePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}/save")
    @Operation(summary = "Unsave a post")
    public ResponseEntity<ApiResponse<PostResponse>> unsavePost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        PostResponse response = postService.unsavePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get saved posts")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getSavedPosts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page") @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.getSavedPosts(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(posts)));
    }

    @PostMapping("/{postId}/repost")
    @Operation(summary = "Repost a post", description = "Share someone else's post to your profile with an optional quote")
    public ResponseEntity<ApiResponse<RepostResponse>> repost(
            @PathVariable("postId") UUID postId,
            @RequestBody(required = false) RepostRequest request,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        String quote = request != null ? request.getQuote() : null;
        RepostResponse response = postService.repost(postId, userId, quote);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{postId}/repost")
    @Operation(summary = "Undo repost")
    public ResponseEntity<ApiResponse<Void>> undoRepost(
            @PathVariable("postId") UUID postId,
            Authentication authentication) {
        UUID userId = SecurityUtils.getUserId(authentication);
        postService.undoRepost(postId, userId);
        return ResponseEntity.ok(ApiResponse.success("Repost removed"));
    }

    @GetMapping("/user/{userId}/reposts")
    @Operation(summary = "Get user's reposts")
    public ResponseEntity<ApiResponse<PagedResponse<RepostResponse>>> getUserReposts(
            @PathVariable("userId") UUID userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of reposts per page") @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        UUID currentUserId = SecurityUtils.getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<RepostResponse> reposts = postService.getUserReposts(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(reposts)));
    }
}
