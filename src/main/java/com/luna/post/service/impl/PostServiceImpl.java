package com.luna.post.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luna.activity.entity.ActivityType;
import com.luna.activity.service.IActivityService;
import com.luna.comment.repository.CommentRepository;
import com.luna.common.exception.BadRequestException;
import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.exception.UnauthorizedException;
import com.luna.common.service.CloudinaryService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {
    
    private static final long MAX_TOTAL_IMAGE_SIZE = 50 * 1024 * 1024; // 50MB total for images
    private static final long MAX_TOTAL_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB total for videos
    
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final IActivityService activityService;
    private final CloudinaryService cloudinaryService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long userId, List<MultipartFile> images, List<MultipartFile> videos) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Validate: can't have both images and videos
        boolean hasImages = images != null && !images.isEmpty() && images.stream().anyMatch(f -> f != null && !f.isEmpty());
        boolean hasVideos = videos != null && !videos.isEmpty() && videos.stream().anyMatch(f -> f != null && !f.isEmpty());
        
        if (hasImages && hasVideos) {
            throw new BadRequestException("Cannot upload both images and videos in the same post");
        }
        
        // Validate total image size
        if (hasImages && images != null) {
            long totalImageSize = images.stream()
                .filter(f -> f != null && !f.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();
            if (totalImageSize > MAX_TOTAL_IMAGE_SIZE) {
                throw new BadRequestException("Total image size exceeds 50MB limit");
            }
        }
        
        // Validate total video size
        if (hasVideos && videos != null) {
            long totalVideoSize = videos.stream()
                .filter(f -> f != null && !f.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();
            if (totalVideoSize > MAX_TOTAL_VIDEO_SIZE) {
                throw new BadRequestException("Total video size exceeds 100MB limit");
            }
        }
        
        Post post = Post.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .author(user)
            .likeCount(0L)
            .build();
        
        // Upload images
        if (hasImages && images != null) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    String imageUrl = cloudinaryService.uploadImage(image, "posts");
                    imageUrls.add(imageUrl);
                }
            }
            if (!imageUrls.isEmpty()) {
                post.setImageUrls(toJson(imageUrls));
            }
        }
        
        // Upload videos
        if (hasVideos && videos != null) {
            List<String> videoUrls = new ArrayList<>();
            for (MultipartFile video : videos) {
                if (video != null && !video.isEmpty()) {
                    String videoUrl = cloudinaryService.uploadVideo(video, "posts");
                    videoUrls.add(videoUrl);
                }
            }
            if (!videoUrls.isEmpty()) {
                post.setVideoUrls(toJson(videoUrls));
            }
        }
        
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
        
        // Delete media from Cloudinary and posts in batches
        int batchSize = 100;
        int totalDeleted = 0;
        
        for (int i = 0; i < postsToDelete.size(); i += batchSize) {
            int end = Math.min(i + batchSize, postsToDelete.size());
            java.util.List<Post> batch = postsToDelete.subList(i, end);
            
            // Delete media for each post in the batch
            for (Post post : batch) {
                deletePostMedia(post);
            }
            
            // Delete posts from database
            postRepository.deleteAll(batch);
            totalDeleted += batch.size();
        }
        
        return totalDeleted;
    }
    
    private void deletePostMedia(Post post) {
        // Delete images
        List<String> imageUrls = fromJson(post.getImageUrls());
        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                String publicId = cloudinaryService.extractPublicId(imageUrl);
                if (publicId != null) {
                    cloudinaryService.deleteFile(publicId);
                }
            }
        }
        
        // Delete videos
        List<String> videoUrls = fromJson(post.getVideoUrls());
        if (videoUrls != null) {
            for (String videoUrl : videoUrls) {
                String publicId = cloudinaryService.extractPublicId(videoUrl);
                if (publicId != null) {
                    cloudinaryService.deleteFile(publicId);
                }
            }
        }
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
        long commentCount = commentRepository.countByPostId(post.getId());
        
        return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .imageUrls(fromJson(post.getImageUrls()))
            .videoUrls(fromJson(post.getVideoUrls()))
            .author(PostResponse.AuthorInfo.builder()
                .id(post.getAuthor().getId())
                .username(post.getAuthor().getUsername())
                .email(post.getAuthor().getEmail())
                .profileImageUrl(post.getAuthor().getProfileImageUrl())
                .build())
            .likeCount(post.getLikeCount())
            .commentCount(commentCount)
            .isLikedByCurrentUser(isLiked)
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
    
    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to process image URLs");
        }
    }
    
    private List<String> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
