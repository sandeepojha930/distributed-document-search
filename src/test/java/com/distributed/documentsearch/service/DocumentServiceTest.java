package com.distributed.documentsearch.service;

import com.distributed.documentsearch.config.TenantContext;
import com.distributed.documentsearch.dto.DocumentRequest;
import com.distributed.documentsearch.dto.DocumentResponse;
import com.distributed.documentsearch.model.Document;
import com.distributed.documentsearch.model.DocumentIndex;
import com.distributed.documentsearch.model.DocumentStatus;
import com.distributed.documentsearch.repository.DocumentIndexRepository;
import com.distributed.documentsearch.repository.DocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private DocumentIndexRepository documentIndexRepository;
    private RabbitTemplate rabbitTemplate;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        documentIndexRepository = mock(DocumentIndexRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        documentService = new DocumentService(documentRepository, documentIndexRepository, rabbitTemplate);

        TenantContext.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createDocument_persistsEntity_andPublishesIndexMessage() {
        DocumentRequest request = new DocumentRequest();
        request.setTitle("Test Title");
        request.setContent("Test Content");

        Document saved = Document.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-test")
                .title("Test Title")
                .content("Test Content")
                .status(DocumentStatus.INDEXING)
                .build();

        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        DocumentResponse response = documentService.createDocument(request);

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getTenantId()).isEqualTo("tenant-test");
        assertThat(response.getTitle()).isEqualTo("Test Title");
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.INDEXING);

        verify(documentRepository, times(1)).save(any(Document.class));
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("document-exchange"), startsWith("document.index."), eq(saved.getId().toString()));
    }

    @Test
    void getDocument_returnsDocument_whenFound() {
        UUID id = UUID.randomUUID();
        Document existing = Document.builder()
                .id(id)
                .tenantId("tenant-test")
                .title("Test")
                .content("Content")
                .status(DocumentStatus.INDEXED)
                .build();

        when(documentRepository.findByIdAndTenantId(id, "tenant-test")).thenReturn(Optional.of(existing));

        DocumentResponse response = documentService.getDocument(id, "tenant-test");

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getTenantId()).isEqualTo("tenant-test");
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.INDEXED);
    }

    @Test
    void getDocument_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findByIdAndTenantId(id, "tenant-test")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> documentService.getDocument(id, "tenant-test"));
    }

    @Test
    void deleteDocument_marksDeleted_deletesFromIndex_andPublishesDeleteMessage() {
        UUID id = UUID.randomUUID();
        Document existing = Document.builder()
                .id(id)
                .tenantId("tenant-test")
                .title("Title")
                .content("Content")
                .status(DocumentStatus.INDEXED)
                .build();

        when(documentRepository.findByIdAndTenantId(id, "tenant-test")).thenReturn(Optional.of(existing));

        documentService.deleteDocument(id, "tenant-test");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(captor.capture());
        Document saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.DELETED);

        verify(documentIndexRepository, times(1)).deleteById(id.toString());
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("document-exchange"), startsWith("document.delete."), eq(id.toString()));
    }

    @Test
    void indexDocument_updatesStatusToIndexed_onSuccess() {
        UUID id = UUID.randomUUID();
        Document existing = Document.builder()
                .id(id)
                .tenantId("tenant-test")
                .title("Title")
                .content("Content")
                .status(DocumentStatus.INDEXING)
                .build();

        when(documentRepository.findById(id)).thenReturn(Optional.of(existing));

        documentService.indexDocument(id);

        verify(documentIndexRepository, times(1)).save(any(DocumentIndex.class));
        verify(documentRepository, atLeastOnce()).save(existing);
        assertThat(existing.getStatus()).isEqualTo(DocumentStatus.INDEXED);
    }

    @Test
    void indexDocument_setsStatusFailed_onError() {
        UUID id = UUID.randomUUID();
        Document existing = Document.builder()
                .id(id)
                .tenantId("tenant-test")
                .title("Title")
                .content("Content")
                .status(DocumentStatus.INDEXING)
                .build();

        when(documentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(documentIndexRepository.save(any(DocumentIndex.class))).thenThrow(new RuntimeException("ES error"));

        assertThrows(RuntimeException.class, () -> documentService.indexDocument(id));
        verify(documentRepository, atLeastOnce()).save(existing);
        assertThat(existing.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }
}

