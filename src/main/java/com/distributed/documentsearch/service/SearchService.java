package com.distributed.documentsearch.service;

import com.distributed.documentsearch.dto.SearchRequest;
import com.distributed.documentsearch.dto.SearchResponse;
import com.distributed.documentsearch.model.DocumentIndex;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Cacheable(
            value = "search",
            key = "#request.tenant + ':' + T(java.util.Objects).hash(#request.q, #request.page, #request.size)"
    )
    @CircuitBreaker(name = "elasticsearch")
    @Retry(name = "elasticsearch")
    public SearchResponse search(SearchRequest request) {

        String tenantId = request.getTenant();
        String queryText = request.getQ() != null ? request.getQ().trim() : "";

        int page = request.getPage() != null ? request.getPage() - 1 : 0;
        int size = request.getSize() != null ? request.getSize() : 10;

        Pageable pageable = PageRequest.of(page, size);

        Criteria criteria = new Criteria("tenantId").is(tenantId);

        if (!queryText.isEmpty()) {
            criteria = criteria.and(
                    new Criteria("title").contains(queryText)
                            .or(new Criteria("content").contains(queryText))
            );
        }

        CriteriaQuery query = new CriteriaQuery(criteria, pageable);

        SearchHits<DocumentIndex> hits =
                elasticsearchOperations.search(query, DocumentIndex.class);

        List<SearchResponse.SearchResult> results = hits.getSearchHits().stream()
                .map(hit -> mapToResult(hit, queryText))
                .toList();

        return SearchResponse.builder()
                .query(queryText)
                .total(hits.getTotalHits())
                .page(page + 1)
                .size(results.size())
                .results(results)
                .build();
    }

    private SearchResponse.SearchResult mapToResult(
            SearchHit<DocumentIndex> hit,
            String query
    ) {
        DocumentIndex doc = hit.getContent();

        return SearchResponse.SearchResult.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .snippet(extractSnippet(doc, query))
                .score(hit.getScore())
                .metadata(doc.getMetadata())
                .build();
    }

    private String extractSnippet(DocumentIndex document, String query) {
        if (query == null || query.isBlank()) {
            return document.getContent();
        }

        String content = document.getContent();
        if (content == null || content.length() <= 150) {
            return content != null ? content : "";
        }

        int index = content.toLowerCase().indexOf(query.toLowerCase());
        if (index < 0) {
            return content.substring(0, 150) + "...";
        }

        int start = Math.max(0, index - 50);
        int end = Math.min(content.length(), index + query.length() + 100);

        String snippet = content.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet += "...";

        return snippet;
    }
}
