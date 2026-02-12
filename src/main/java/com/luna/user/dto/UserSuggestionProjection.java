package com.luna.user.dto;

public interface UserSuggestionProjection {
    Long getId();
    String getUsername();
    String getProfileImageUrl();
    String getCountryCode();
    Integer getMutualConnectionCount();
    Long getFollowerCount();
}
