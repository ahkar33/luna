package com.luna.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuggestionResponse {
    private Long id;
    private String username;
    private String profileImageUrl;
    private Long followerCount;
    private Integer mutualConnections;
    private String suggestionReason;  // e.g., "Popular", "Followed by user1, user2"
}
