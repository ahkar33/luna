package com.luna.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response metadata")
public class ResponseMeta {
    
    @Schema(description = "Response timestamp", example = "2026-01-23T10:30:00Z")
    @Builder.Default
    private String timestamp = Instant.now().toString();
    
    @Schema(description = "Request ID for tracking", example = "req_abc123")
    private String requestId;
    
    @Schema(description = "Pagination information")
    private PaginationMeta pagination;
}
