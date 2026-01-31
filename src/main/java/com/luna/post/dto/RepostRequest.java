package com.luna.post.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepostRequest {
    
    @Size(max = 500, message = "Quote must be less than 500 characters")
    private String quote;  // Optional quote when reposting
}
