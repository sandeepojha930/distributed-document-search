package com.distributed.documentsearch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    
    @NotBlank(message = "Query is required")
    private String q;
    
    @NotBlank(message = "Tenant ID is required")
    private String tenant;
    
    @Min(1)
    private Integer page = 1;
    
    @Min(1)
    private Integer size = 10;
    
    private String sort = "relevance";
}
