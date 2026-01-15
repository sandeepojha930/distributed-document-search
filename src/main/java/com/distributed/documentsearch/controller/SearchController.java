package com.distributed.documentsearch.controller;

import com.distributed.documentsearch.dto.SearchRequest;
import com.distributed.documentsearch.dto.SearchResponse;
import com.distributed.documentsearch.service.RateLimitService;
import com.distributed.documentsearch.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for document search operations in the distributed document search system.
 *
 * This controller provides full-text search capabilities across indexed documents
 * with relevance ranking, pagination, and multi-tenant support.
 *
 * Base path: /api/v1/search
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    /** Service for executing search queries against Elasticsearch */
    private final SearchService searchService;

    /** Service for rate limiting functionality */
    private final RateLimitService rateLimitService;

    /**
     * Performs a full-text search across documents for the specified tenant.
     *
     * This endpoint searches both document titles and content using Elasticsearch,
     * returning results with relevance scoring, snippets, and pagination support.
     * Results are filtered by tenant for data isolation.
     *
     * @param request the search request containing query, tenant, and pagination parameters
     * @return ResponseEntity containing search results with relevance-ranked documents
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @GetMapping
    public ResponseEntity<SearchResponse> search(@Valid SearchRequest request) {
        if (!rateLimitService.isAllowed(request.getTenant())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }
}
