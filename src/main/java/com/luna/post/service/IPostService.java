package com.luna.post.service;

import com.luna.post.dto.CreatePostRequest;
import com.luna.post.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostService {
    
    PostResponse createPost(CreatePostRequest request, Long userId, List<MultipartFile> images, List<MultipartFile> videos);
    
    PostResponse getPostById(Long postId, Long currentUserId);
    
    Page<PostResponse> getUserPosts(Long userId, Long currentUserId, Pageable pageable);
    
    Page<PostResponse> getTimelinePosts(Long userId, Pageable pageable);
    
    void deletePost(Long postId, Long userId);
    
    void restorePost(Long postId, Long userId);
    
    int cleanupOldDeletedPosts();
    
    PostResponse likePost(Long postId, Long userId);
    
    PostResponse unlikePost(Long postId, Long userId);
    
    PostResponse savePost(Long postId, Long userId);
    
    PostResponse unsavePost(Long postId, Long userId);
    
    Page<PostResponse> getSavedPosts(Long userId, Pageable pageable);
}
