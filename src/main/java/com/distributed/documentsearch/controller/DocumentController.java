package com.distributed.documentsearch.controller;

import com.distributed.documentsearch.config.TenantContext;
import com.distributed.documentsearch.dto.DocumentRequest;
import com.distributed.documentsearch.dto.DocumentResponse;
import com.distributed.documentsearch.service.DocumentService;
import com.distributed.documentsearch.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing documents in the distributed document search system.
 *
 * This controller provides HTTP endpoints for document operations including creation,
 * retrieval, and deletion. It implements multi-tenant support through tenant context
 * and includes rate limiting for API protection.
 *
 * Base path: /api/v1/documents
 *
 * Key Features:
 * - Document creation with automatic indexing
 * - Document retrieval by ID with tenant isolation
 * - Document deletion with index cleanup
 * - Rate limiting per tenant
 * - Input validation and error handling
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    /** Service for document business logic operations */
    private final DocumentService documentService;

    /** Service for rate limiting functionality */
    private final RateLimitService rateLimitService;

    /**
     * Creates a new document for the current tenant.
     *
     * This endpoint accepts a document creation request, validates it, applies rate limiting,
     * and initiates the document creation process. The document is stored in the database
     * and queued for asynchronous indexing in Elasticsearch.
     *
     * @param request the document creation request containing title, content, and metadata
     * @return ResponseEntity containing the created document response with HTTP 201 status
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        String tenantId = TenantContext.getTenantId();

        if (!rateLimitService.isAllowed(tenantId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        DocumentResponse response = documentService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a document by its ID for the current tenant.
     *
     * This endpoint fetches a document from the database using both the document ID
     * and the current tenant ID for proper data isolation. The document must belong
     * to the requesting tenant.
     *
     * @param id the unique identifier of the document to retrieve
     * @return ResponseEntity containing the document response if found, or HTTP 404 if not found
     * @throws DocumentNotFoundException if the document does not exist or belongs to a different tenant
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        String tenantId = TenantContext.getTenantId();

        if (!rateLimitService.isAllowed(tenantId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            DocumentResponse response = documentService.getDocument(id, tenantId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a document by its ID for the current tenant.
     *
     * This endpoint removes a document from the database and initiates cleanup
     * of the document from the Elasticsearch index. The operation ensures that
     * only documents belonging to the current tenant can be deleted.
     *
     * @param id the unique identifier of the document to delete
     * @return ResponseEntity with HTTP 204 (No Content) if successful, or HTTP 404 if not found
     * @throws DocumentNotFoundException if the document does not exist or belongs to a different tenant
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        String tenantId = TenantContext.getTenantId();
        
        if (!rateLimitService.isAllowed(tenantId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        try {
            documentService.deleteDocument(id, tenantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
