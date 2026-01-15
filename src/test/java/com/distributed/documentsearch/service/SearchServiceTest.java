package com.distributed.documentsearch.service;

import com.distributed.documentsearch.dto.SearchRequest;
import com.distributed.documentsearch.dto.SearchResponse;
import com.distributed.documentsearch.model.DocumentIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private ElasticsearchOperations elasticsearchOperations;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        elasticsearchOperations = mock(ElasticsearchOperations.class);
        searchService = new SearchService(elasticsearchOperations);
    }

    @Test
    void search_returnsMappedResults() {
        SearchRequest request = new SearchRequest();
        request.setTenant("tenant-1");
        request.setQ("test");
        request.setPage(1);
        request.setSize(10);

        DocumentIndex doc = DocumentIndex.builder()
                .id("1")
                .tenantId("tenant-1")
                .title("Test Document")
                .content("This is test content")
                .metadata(null)
                .build();

        SearchHit<DocumentIndex> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hit.getScore()).thenReturn(1.0f);

        SearchHits<DocumentIndex> hits = mock(SearchHits.class);
        when(hits.getTotalHits()).thenReturn(1L);
        when(hits.getSearchHits()).thenReturn(List.of(hit));

        when(elasticsearchOperations.search(any(CriteriaQuery.class), eq(DocumentIndex.class)))
                .thenReturn(hits);

        SearchResponse response = searchService.search(request);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getTitle()).isEqualTo("Test Document");
        assertThat(response.getResults().get(0).getId()).isEqualTo("1");
    }
}
