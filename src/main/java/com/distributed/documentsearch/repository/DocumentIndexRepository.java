package com.distributed.documentsearch.repository;

import com.distributed.documentsearch.model.DocumentIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.annotations.Query;

public interface DocumentIndexRepository
        extends ElasticsearchRepository<DocumentIndex, String> {

    Page<DocumentIndex> findByTenantId(String tenantId, Pageable pageable);

    Page<DocumentIndex> findByTenantIdAndTitleContainingOrContentContaining(
            String tenantId, String titleQuery, String contentQuery, Pageable pageable);
}
