package com.luna.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBioRequest {
    
    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;
}
