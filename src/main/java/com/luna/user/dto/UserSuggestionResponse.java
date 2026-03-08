package com.luna.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuggestionResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String profileImageUrl;
    private Long followerCount;
    private Integer mutualConnections;
    private String suggestionReason;
    private List<String> mutualConnectionUsernames;
    private Boolean isFollowing;
}
