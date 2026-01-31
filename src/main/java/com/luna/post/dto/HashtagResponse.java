package com.luna.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponse {
    private Long id;
    private String name;
    private Long postCount;  // Number of posts using this hashtag
}
