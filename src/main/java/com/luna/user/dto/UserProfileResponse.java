package com.luna.user.dto;

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
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String email;
    private String profileImageUrl;
    private String bio;
    private String countryCode;
    private String country;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
    private Long followerCount;
    private Long followingCount;
    private Long postCount;
    private Boolean isMyProfile;
}
