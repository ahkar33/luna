package com.luna.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
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
}
