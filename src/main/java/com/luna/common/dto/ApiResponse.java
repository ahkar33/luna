package com.luna.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    
    @Schema(description = "Indicates if the request was successful", example = "true")
    private Boolean success;
    
    @Schema(description = "Response data (only present on success)")
    private T data;
    
    @Schema(description = "Success message (optional)", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "Error details (only present on failure)")
    private ErrorDetails error;
    
    @Schema(description = "Response metadata")
    private ResponseMeta meta;
    
    // Success response with data
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(ResponseMeta.builder().build())
                .build();
    }
    
    // Success response with data and message
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .meta(ResponseMeta.builder().build())
                .build();
    }
    
    // Success response with message only
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .meta(ResponseMeta.builder().build())
                .build();
    }
    
    // Error response
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .meta(ResponseMeta.builder().build())
                .build();
    }
    
    // Error response with details
    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .meta(ResponseMeta.builder().build())
                .build();
    }
    
    // Error response with field (for validation errors)
    public static <T> ApiResponse<T> error(String code, String message, String field) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .field(field)
                        .build())
                .meta(ResponseMeta.builder().build())
                .build();
    }
}
