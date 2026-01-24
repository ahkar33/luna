package com.luna.post.service.impl;

import com.luna.activity.entity.ActivityType;
import com.luna.activity.service.IActivityService;
import com.luna.common.exception.BadRequestException;
import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.exception.UnauthorizedException;
import com.luna.post.dto.CreatePostRequest;
import com.luna.post.dto.PostResponse;
import com.luna.post.entity.Post;
import com.luna.post.entity.PostLike;
import com.luna.post.repository.PostLikeRepository;
import com.luna.post.repository.PostRepository;
import com.luna.post.service.IPostService;
import com.luna.user.entity.User;
import com.luna.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {
    
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final IActivityService activityService;
    
    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Post post = Post.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .author(user)
            .likeCount(0L)
            .build();
        
        post = postRepository.save(post);
        
        // Log activity
        activityService.logActivity(userId, ActivityType.POST_CREATE, "POST", 
            post.getId(), null, null);
        
        return mapToPostResponse(post, false);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (post.isDeleted()) {
            throw new ResourceNotFoundException("Post not found");
        }
        
        boolean isLiked = currentUserId != null && 
            postLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
        
        return mapToPostResponse(post, isLiked);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Long currentUserId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        
        Page<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        
        return posts.map(post -> {
            boolean isLiked = currentUserId != null && 
                postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
            return mapToPostResponse(post, isLiked);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getTimelinePosts(Long userId, Pageable pageable) {
        Page<Post> posts = postRepository.findTimelinePosts(userId, pageable);
        
        return posts.map(post -> {
            boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);
            return mapToPostResponse(post, isLiked);
        });
    }
    
    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (!post.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own posts");
        }
        
        if (post.isDeleted()) {
            throw new BadRequestException("Post is already deleted");
        }
        
        post.setDeletedAt(java.time.LocalDateTime.now());
        postRepository.save(post);
        
        // Log activity
        activityService.logActivity(userId, ActivityType.POST_DELETE, "POST", 
            postId, null, null);
    }
    
    @Override
    @Transactional
    public void restorePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (!post.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only restore your own posts");
        }
        
        if (!post.isDeleted()) {
            throw new BadRequestException("Post is not deleted");
        }
        
        if (post.isPermanentlyDeletable()) {
            throw new BadRequestException("Post cannot be restored after 30 days");
        }
        
        post.setDeletedAt(null);
        postRepository.save(post);
    }
    
    @Override
    @Transactional
    public int cleanupOldDeletedPosts() {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(30);
        java.util.List<Post> postsToDelete = postRepository.findPostsToHardDelete(cutoffDate);
        
        if (postsToDelete.isEmpty()) {
            return 0;
        }
        
        // Delete in batches to avoid memory issues with large datasets
        int batchSize = 100;
        int totalDeleted = 0;
        
        for (int i = 0; i < postsToDelete.size(); i += batchSize) {
            int end = Math.min(i + batchSize, postsToDelete.size());
            java.util.List<Post> batch = postsToDelete.subList(i, end);
            postRepository.deleteAll(batch);
            totalDeleted += batch.size();
        }
        
        return totalDeleted;
    }
    
    @Override
    @Transactional
    public PostResponse likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (post.isDeleted()) {
            throw new ResourceNotFoundException("Post not found");
        }
        
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BadRequestException("You have already liked this post");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        PostLike postLike = PostLike.builder()
            .post(post)
            .user(user)
            .build();
        
        postLikeRepository.save(postLike);
        
        post.setLikeCount(post.getLikeCount() + 1);
        post = postRepository.save(post);
        
        // Log activity
        activityService.logActivity(userId, ActivityType.LIKE, "POST", 
            postId, post.getAuthor().getId(), null);
        
        return mapToPostResponse(post, true);
    }
    
    @Override
    @Transactional
    public PostResponse unlikePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (post.isDeleted()) {
            throw new ResourceNotFoundException("Post not found");
        }
        
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BadRequestException("You have not liked this post");
        }
        
        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        post = postRepository.save(post);
        
        // Log activity
        activityService.logActivity(userId, ActivityType.UNLIKE, "POST", 
            postId, post.getAuthor().getId(), null);
        
        return mapToPostResponse(post, false);
    }
    
    private PostResponse mapToPostResponse(Post post, boolean isLiked) {
        return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .author(PostResponse.AuthorInfo.builder()
                .id(post.getAuthor().getId())
                .username(post.getAuthor().getUsername())
                .email(post.getAuthor().getEmail())
                .build())
            .likeCount(post.getLikeCount())
            .isLikedByCurrentUser(isLiked)
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}
