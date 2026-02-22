package com.luna.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must be less than 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;
}
