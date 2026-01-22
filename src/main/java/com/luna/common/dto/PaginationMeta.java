package com.luna.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Pagination metadata")
public class PaginationMeta {
    
    @Schema(description = "Current page number", example = "1")
    private Integer page;
    
    @Schema(description = "Items per page", example = "10")
    private Integer limit;
    
    @Schema(description = "Total number of items", example = "25")
    private Long total;
    
    @Schema(description = "Total number of pages", example = "3")
    private Integer totalPages;
}
