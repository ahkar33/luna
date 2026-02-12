package com.luna.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private String suggestionReason;  // e.g., "Popular", "Followed by john_doe, jane_doe, and 3 others"
    private List<String> mutualConnectionUsernames;  // e.g., ["john_doe", "jane_doe"]
    private Boolean isFollowing;  // Whether current user follows this suggested user
}
