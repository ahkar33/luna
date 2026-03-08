package com.luna.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepostResponse {

    private UUID id;
    private String quote;
    private RepostAuthor repostedBy;
    private PostResponse originalPost;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepostAuthor {
        private UUID id;
        private String username;
        private String profileImageUrl;
    }
}
