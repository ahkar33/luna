package com.luna.post.service;

import com.luna.post.dto.CreatePostRequest;
import com.luna.post.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPostService {
    
    PostResponse createPost(CreatePostRequest request, Long userId);
    
    PostResponse getPostById(Long postId, Long currentUserId);
    
    Page<PostResponse> getUserPosts(Long userId, Long currentUserId, Pageable pageable);
    
    Page<PostResponse> getTimelinePosts(Long userId, Pageable pageable);
    
    void deletePost(Long postId, Long userId);
    
    void restorePost(Long postId, Long userId);
    
    int cleanupOldDeletedPosts();
    
    PostResponse likePost(Long postId, Long userId);
    
    PostResponse unlikePost(Long postId, Long userId);
}
