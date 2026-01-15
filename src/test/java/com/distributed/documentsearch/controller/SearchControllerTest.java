package com.distributed.documentsearch.controller;

import com.distributed.documentsearch.dto.SearchRequest;
import com.distributed.documentsearch.dto.SearchResponse;
import com.distributed.documentsearch.service.RateLimitService;
import com.distributed.documentsearch.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(rateLimitService.isAllowed(any())).thenReturn(true);
    }

    @Test
    void search_returnsResults() throws Exception {
        SearchResponse.SearchResult result = SearchResponse.SearchResult.builder()
                .id("1")
                .title("Test Doc")
                .snippet("snippet")
                .score(1.0f)
                .metadata(Collections.emptyMap())
                .build();

        SearchResponse response = SearchResponse.builder()
                .query("test")
                .total(1L)
                .page(1)
                .size(10)
                .results(Collections.singletonList(result))
                .build();

        when(searchService.search(any(SearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "test")
                        .param("tenant", "tenant-1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.results[0].title").value("Test Doc"));
    }
}

