package com.luna.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    
    private Long id;
    private String title;
    private String content;
    private List<String> imageUrls;
    private List<String> videoUrls;
    private AuthorInfo author;
    private Long likeCount;
    private Long commentCount;
    private Boolean isLikedByCurrentUser;
    private Boolean isSavedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private Long id;
        private String username;
        private String email;
        private String profileImageUrl;
    }
}
