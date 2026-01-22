package com.luna.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Generic message response")
public class MessageResponse {
    
    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;
}
