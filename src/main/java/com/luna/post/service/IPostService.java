package com.luna.post.service;

import com.luna.post.dto.CreatePostRequest;
import com.luna.post.dto.PostResponse;
import com.luna.post.dto.RepostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IPostService {

    PostResponse createPost(CreatePostRequest request, UUID userId, List<MultipartFile> images, List<MultipartFile> videos);

    PostResponse getPostById(UUID postId, UUID currentUserId);

    Page<PostResponse> getUserPosts(UUID userId, UUID currentUserId, Pageable pageable);

    Page<PostResponse> getTimelinePosts(UUID userId, Pageable pageable);

    void deletePost(UUID postId, UUID userId);

    void restorePost(UUID postId, UUID userId);

    int cleanupOldDeletedPosts();

    PostResponse likePost(UUID postId, UUID userId);

    PostResponse unlikePost(UUID postId, UUID userId);

    PostResponse savePost(UUID postId, UUID userId);

    PostResponse unsavePost(UUID postId, UUID userId);

    Page<PostResponse> getSavedPosts(UUID userId, Pageable pageable);

    RepostResponse repost(UUID postId, UUID userId, String quote);

    void undoRepost(UUID postId, UUID userId);

    Page<RepostResponse> getUserReposts(UUID userId, UUID currentUserId, Pageable pageable);
}
