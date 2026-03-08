package com.luna.user.dto;

import java.util.UUID;

public interface UserSuggestionProjection {
    UUID getId();
    String getUsername();
    String getDisplayName();
    String getProfileImageUrl();
    String getCountryCode();
    Integer getMutualConnectionCount();
    Long getFollowerCount();
}
