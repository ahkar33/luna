package com.luna.post.controller;

import com.luna.post.dto.HashtagResponse;
import com.luna.post.dto.PostResponse;
import com.luna.post.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
@Tag(name = "Hashtags", description = "Hashtag discovery and search")
@SecurityRequirement(name = "bearerAuth")
public class HashtagController {
    
    private final HashtagService hashtagService;
    
    @GetMapping("/trending")
    @Operation(summary = "Get trending hashtags", description = "Returns most used hashtags in the last 24 hours")
    public ResponseEntity<List<HashtagResponse>> getTrendingHashtags(
            @RequestParam(defaultValue = "10") int limit) {
        List<HashtagResponse> trending = hashtagService.getTrendingHashtags(Math.min(limit, 50));
        return ResponseEntity.ok(trending);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search hashtags", description = "Search hashtags by prefix")
    public ResponseEntity<List<HashtagResponse>> searchHashtags(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        List<HashtagResponse> results = hashtagService.searchHashtags(q, Math.min(limit, 50));
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/{hashtag}/posts")
    @Operation(summary = "Get posts by hashtag", description = "Returns posts containing the specified hashtag")
    public ResponseEntity<Page<PostResponse>> getPostsByHashtag(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<PostResponse> posts = hashtagService.getPostsByHashtag(hashtag, userId, pageable);
        return ResponseEntity.ok(posts);
    }
}
