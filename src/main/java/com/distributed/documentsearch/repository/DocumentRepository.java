package com.distributed.documentsearch.repository;

import com.distributed.documentsearch.model.Document;
import com.distributed.documentsearch.model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Document entity operations.
 * <p>
 * This interface extends JpaRepository to provide CRUD operations for documents
 * with additional custom query methods for multi-tenant data access.
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Finds a document by its ID and tenant ID for data isolation.
     *
     * @param id       the unique identifier of the document
     * @param tenantId the tenant identifier for multi-tenant isolation
     * @return Optional containing the document if found, empty otherwise
     */
    Optional<Document> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Finds all documents for a tenant with a specific status.
     *
     * @param tenantId the tenant identifier
     * @param status   the document status to filter by
     * @return list of documents matching the criteria
     */
    List<Document> findByTenantIdAndStatus(String tenantId, DocumentStatus status);

    /**
     * Deletes a document by its ID and tenant ID.
     *
     * @param id       the unique identifier of the document
     * @param tenantId the tenant identifier for data isolation
     */
    void deleteByIdAndTenantId(UUID id, String tenantId);
}
