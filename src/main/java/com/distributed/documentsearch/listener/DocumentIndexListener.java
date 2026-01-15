package com.distributed.documentsearch.listener;

import com.distributed.documentsearch.config.RabbitMQConfig;
import com.distributed.documentsearch.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexListener {

    private final DocumentService documentService;

    @RabbitListener(queues = RabbitMQConfig.INDEX_QUEUE)
    public void handleIndexMessage(String documentId) {
        UUID id = UUID.fromString(documentId);
        log.info("Received indexing request for document={}", id);
        documentService.indexDocument(id);
    }

    @RabbitListener(queues = RabbitMQConfig.DELETE_QUEUE)
    public void handleDeleteMessage(String documentId) {
        UUID id = UUID.fromString(documentId);
        log.info("Received deletion request for document: {}", id);
        documentService.deleteIndex(id);
    }

    @PostConstruct
    void init() {
        log.info("DocumentIndexListener initialized");
    }
}
