package com.distributed.documentsearch.controller;

import com.distributed.documentsearch.config.TenantContext;
import com.distributed.documentsearch.dto.DocumentRequest;
import com.distributed.documentsearch.dto.DocumentResponse;
import com.distributed.documentsearch.model.DocumentStatus;
import com.distributed.documentsearch.service.DocumentService;
import com.distributed.documentsearch.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(rateLimitService.isAllowed(any())).thenReturn(true);
    }

    @Test
    void createDocument_returnsCreated() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setTitle("Test");
        request.setContent("Content");

        DocumentResponse response = DocumentResponse.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .title("Test")
                .content("Content")
                .status(DocumentStatus.INDEXING)
                .build();

        when(documentService.createDocument(any(DocumentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/documents")
                        .header("X-Tenant-Id", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test"));
    }

    @Test
    void getDocument_returnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.getDocument(id, "tenant-1")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/v1/documents/{id}", id)
                        .header("X-Tenant-Id", "tenant-1"))
                .andExpect(status().isNotFound());
    }
}

