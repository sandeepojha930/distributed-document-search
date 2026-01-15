package com.distributed.documentsearch.service;

import com.distributed.documentsearch.config.RabbitMQConfig;
import com.distributed.documentsearch.config.TenantContext;
import com.distributed.documentsearch.dto.DocumentRequest;
import com.distributed.documentsearch.dto.DocumentResponse;
import com.distributed.documentsearch.exception.DocumentNotFoundException;
import com.distributed.documentsearch.model.Document;
import com.distributed.documentsearch.model.DocumentIndex;
import com.distributed.documentsearch.model.DocumentStatus;
import com.distributed.documentsearch.repository.DocumentIndexRepository;
import com.distributed.documentsearch.repository.DocumentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service class for document management operations in the distributed document search system.
 *
 * This service handles all document-related business logic including creation, retrieval,
 * deletion, and indexing. It implements fault tolerance patterns and integrates with
 * multiple data stores (PostgreSQL, Elasticsearch, Redis) and messaging (RabbitMQ).
 *
 * Key Features:
 * - Document CRUD operations with tenant isolation
 * - Asynchronous document indexing via RabbitMQ
 * - Redis caching for performance optimization
 * - Circuit breaker and retry patterns for resilience
 * - Multi-tenant data isolation
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    /** Repository for document persistence operations */
    private final DocumentRepository documentRepository;

    /** Repository for Elasticsearch document indexing */
    private final DocumentIndexRepository documentIndexRepository;

    /** RabbitMQ template for sending indexing messages */
    private final RabbitTemplate rabbitTemplate;

    /**
     * Creates a new document for the specified tenant.
     *
     * This method creates a document entity, saves it to the database with INDEXING status,
     * and publishes an indexing task to RabbitMQ for asynchronous processing. The document
     * will be indexed in Elasticsearch by the background indexing service.
     *
     * @param request the document creation request containing title, content, and metadata
     * @return DocumentResponse containing the created document details
     * @throws org.springframework.dao.DataIntegrityViolationException if database constraints are violated
     */
    @Transactional
    @CircuitBreaker(name = "postgresql")
    @Retry(name = "postgresql")
    public DocumentResponse createDocument(DocumentRequest request) {

        String tenantId = TenantContext.getTenantId();

        Document document = Document.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .title(request.getTitle())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .status(DocumentStatus.INDEXING)
                .build();

        document = documentRepository.saveAndFlush(document);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    "document.index." + tenantId,
                    document.getId().toString()
            );
            log.info("Published indexing task for document: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to publish indexing task", e);
        }

        return mapToResponse(document);
    }

    /**
     * Retrieves a document by its ID for the specified tenant.
     *
     * This method fetches a document from the database using both the document ID
     * and tenant ID to ensure proper data isolation. Results are cached in Redis
     * for improved performance.
     *
     * @param id the unique identifier of the document
     * @param tenantId the tenant identifier for data isolation
     * @return DocumentResponse containing the document details
     * @throws DocumentNotFoundException if the document does not exist or belongs to a different tenant
     */
    @Cacheable(
            value = "documents",
            key = "#id + ':' + #tenantId",
            unless = "#result == null"
    )
    public DocumentResponse getDocument(UUID id, String tenantId) {
        Document document = documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        return mapToResponse(document);
    }

    /**
     * Deletes a document by its ID for the specified tenant.
     *
     * This method marks the document as DELETED in the database and removes it
     * from the Elasticsearch index. The cache entry is also evicted to ensure
     * consistency.
     *
     * @param id the unique identifier of the document to delete
     * @param tenantId the tenant identifier for data isolation
     * @throws DocumentNotFoundException if the document does not exist or belongs to a different tenant
     */
    @Transactional
    @CacheEvict(value = "documents", key = "#id + ':' + #tenantId")
    public void deleteDocument(UUID id, String tenantId) {

        Document document = documentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        document.setStatus(DocumentStatus.DELETED);
        documentRepository.save(document);
        documentRepository.deleteByIdAndTenantId(id, tenantId);

        try {
            documentIndexRepository.deleteById(id.toString());
            log.info("Deleted document from index: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete document from index", e);
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_EXCHANGE,
                    "document.delete." + tenantId,
                    id.toString()
            );
        } catch (Exception e) {
            log.error("Failed to publish deletion task", e);
        }
    }

    /**
     * Indexes a document in Elasticsearch.
     *
     * This method is called asynchronously by the RabbitMQ listener to index
     * documents in Elasticsearch after they are created. It retrieves the document
     * from the database, converts it to an indexable format, and saves it to
     * Elasticsearch. The document status is updated to INDEXED upon success.
     *
     * @param documentId the unique identifier of the document to index
     * @throws RuntimeException if the document is not found or indexing fails
     */
    @Transactional
    public void indexDocument(UUID documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        try {
            DocumentIndex index = DocumentIndex.builder()
                    .id(document.getId().toString())
                    .tenantId(document.getTenantId())
                    .title(document.getTitle())
                    .content(document.getContent())
                    .metadata(document.getMetadata())
                    .createdAt(document.getCreatedAt() != null ? document.getCreatedAt().toString() : null)
                    .updatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt().toString() : null)
                    .build();

            documentIndexRepository.save(index);

            document.setStatus(DocumentStatus.INDEXED);
            documentRepository.save(document);

            log.info("Successfully indexed document: {}", documentId);

        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            log.error("Failed to index document: {}", documentId, e);
            throw e;
        }
    }

    /**
     * Called ONLY by RabbitMQ listener
     */
    @Transactional
    public void deleteIndex(UUID documentId) {
        try {
            documentIndexRepository.deleteById(documentId.toString());
            log.info("Deleted document from Elasticsearch index: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document from Elasticsearch index: {}", documentId, e);
            throw e;
        }
    }

    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .tenantId(document.getTenantId())
                .title(document.getTitle())
                .content(document.getContent())
                .status(document.getStatus())
                .metadata(document.getMetadata())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

}
