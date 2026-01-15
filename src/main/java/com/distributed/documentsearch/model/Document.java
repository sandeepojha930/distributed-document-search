package com.distributed.documentsearch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing a document in the distributed document search system.
 *
 * This entity maps to the 'documents' table in PostgreSQL and includes
 * multi-tenant support, audit timestamps, and indexing status tracking.
 * Documents are automatically indexed in Elasticsearch for search functionality.
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /** Unique identifier for the document */
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Tenant identifier for multi-tenant data isolation */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /** Document title for search and display */
    @Column(nullable = false)
    private String title;

    /** Full text content of the document */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** Current indexing status of the document */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.INDEXING;

    /** Additional metadata stored as JSONB in PostgreSQL */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /** Timestamp when the document was created */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the document was last updated */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
