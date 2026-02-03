package com.luna.post.service;

import com.luna.post.dto.HashtagResponse;
import com.luna.post.dto.PostResponse;
import com.luna.post.entity.Hashtag;
import com.luna.post.entity.Post;
import com.luna.post.entity.PostHashtag;
import com.luna.post.repository.HashtagRepository;
import com.luna.post.repository.PostHashtagRepository;
import com.luna.post.repository.PostRepository;
import com.luna.post.repository.PostLikeRepository;
import com.luna.comment.repository.CommentRepository;
import com.luna.post.repository.SavedPostRepository;
import com.luna.post.repository.RepostRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HashtagService {
    
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
    
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final SavedPostRepository savedPostRepository;
    private final RepostRepository repostRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Extract hashtags from content and link them to the post
     */
    @Transactional
    public void processHashtags(Post post) {
        String content = post.getContent();
        if (content == null || content.isEmpty()) {
            return;
        }
        
        // Delete existing hashtags for this post (in case of update)
        postHashtagRepository.deleteByPostId(post.getId());
        
        // Extract unique hashtags
        Set<String> hashtagNames = extractHashtags(content);
        
        for (String name : hashtagNames) {
            // Find or create hashtag
            Hashtag hashtag = hashtagRepository.findByName(name)
                .orElseGet(() -> hashtagRepository.save(
                    Hashtag.builder().name(name).build()
                ));
            
            // Link to post
            PostHashtag postHashtag = PostHashtag.builder()
                .post(post)
                .hashtag(hashtag)
                .build();
            postHashtagRepository.save(postHashtag);
        }
    }
    
    /**
     * Extract hashtag names from content (lowercase, without #)
     */
    public Set<String> extractHashtags(String content) {
        Set<String> hashtags = new HashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase();
            if (tag.length() <= 50) {  // Limit hashtag length
                hashtags.add(tag);
            }
        }
        
        return hashtags;
    }
    
    /**
     * Get trending hashtags (most used in last 24 hours)
     */
    @Transactional(readOnly = true)
    public List<HashtagResponse> getTrendingHashtags(int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Hashtag> trending = hashtagRepository.findTrendingHashtags(since, PageRequest.of(0, limit));
        
        return trending.stream()
            .map(h -> HashtagResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .postCount(postHashtagRepository.countRecentPostsByHashtagId(h.getId(), since))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Search hashtags by prefix
     */
    @Transactional(readOnly = true)
    public List<HashtagResponse> searchHashtags(String query, int limit) {
        String prefix = query.toLowerCase().replaceAll("^#", "");
        List<Hashtag> hashtags = hashtagRepository.findByNameStartingWithOrderByNameAsc(
            prefix, PageRequest.of(0, limit)
        );
        
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return hashtags.stream()
            .map(h -> HashtagResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .postCount(postHashtagRepository.countRecentPostsByHashtagId(h.getId(), since))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Get posts by hashtag
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByHashtag(String hashtagName, Long currentUserId, Pageable pageable) {
        String name = hashtagName.toLowerCase().replaceAll("^#", "");
        
        Page<Post> posts = postRepository.findByHashtag(name, pageable);
        
        return posts.map(post -> mapToPostResponse(post, currentUserId));
    }
    
    private PostResponse mapToPostResponse(Post post, Long userId) {
        boolean isLiked = userId != null && postLikeRepository.existsByPostIdAndUserId(post.getId(), userId);
        boolean isSaved = userId != null && savedPostRepository.existsByUserIdAndPostId(userId, post.getId());
        boolean isReposted = userId != null && repostRepository.existsByUserIdAndOriginalPostId(userId, post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());
        long repostCount = repostRepository.countByOriginalPostId(post.getId());
        
        return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .imageUrls(fromJson(post.getImageUrls()))
            .videoUrls(fromJson(post.getVideoUrls()))
            .author(PostResponse.AuthorInfo.builder()
                .id(post.getAuthor().getId())
                .username(post.getAuthor().getUsernameField())
                .email(post.getAuthor().getEmail())
                .profileImageUrl(post.getAuthor().getProfileImageUrl())
                .build())
            .likeCount(post.getLikeCount())
            .commentCount(commentCount)
            .repostCount(repostCount)
            .isLikedByCurrentUser(isLiked)
            .isSavedByCurrentUser(isSaved)
            .isRepostedByCurrentUser(isReposted)
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
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
