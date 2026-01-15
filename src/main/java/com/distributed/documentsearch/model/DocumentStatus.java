package com.distributed.documentsearch.model;

/**
 * Enumeration representing the indexing status of a document.
 *
 * This enum tracks the lifecycle of document processing from creation
 * through indexing to final states.
 *
 * @author Distributed Document Search Team
 * @version 1.0
 * @since 1.0
 */
public enum DocumentStatus {

    /** Document is being processed for indexing */
    INDEXING,

    /** Document has been successfully indexed in Elasticsearch */
    INDEXED,

    /** Document indexing failed */
    FAILED,

    /** Document has been marked for deletion */
    DELETED
}
