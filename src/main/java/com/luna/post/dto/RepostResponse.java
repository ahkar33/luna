package com.luna.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepostResponse {
    
    private Long id;
    private String quote;
    private RepostAuthor repostedBy;
    private PostResponse originalPost;
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepostAuthor {
        private Long id;
        private String username;
        private String profileImageUrl;
    }
}
