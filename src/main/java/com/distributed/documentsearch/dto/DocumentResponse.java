package com.distributed.documentsearch.dto;

import com.distributed.documentsearch.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for document response information.
 *
 * This class represents the response structure returned by document-related API endpoints,
 * containing all document details including metadata and timestamps.
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    /** Unique identifier of the document */
    private UUID id;

    /** Tenant identifier for multi-tenant isolation */
    private String tenantId;

    /** Title of the document */
    private String title;

    /** Full text content of the document */
    private String content;

    /** Current processing status of the document */
    private DocumentStatus status;

    /** Additional metadata associated with the document */
    private Map<String, Object> metadata;

    /** Timestamp when the document was created */
    private LocalDateTime createdAt;

    /** Timestamp when the document was last updated */
    private LocalDateTime updatedAt;
}
