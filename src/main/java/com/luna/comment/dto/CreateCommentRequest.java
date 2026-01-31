package com.luna.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    
    @NotBlank(message = "Comment content is required")
    @Size(max = 2000, message = "Comment must be less than 2000 characters")
    private String content;
    
    private Long parentId;  // null for top-level comments
}
