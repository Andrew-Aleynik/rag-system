package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.request.collection.CollectionDeleteRequest;
import com.andrewaleynik.ragsystem.app.dto.request.collection.CollectionRetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.request.collection.CollectionUpdateRequest;
import com.andrewaleynik.ragsystem.app.dto.response.CollectionListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.CollectionResponse;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.TaskStatusResponse;
import com.andrewaleynik.ragsystem.app.services.CollectionCrudService;
import com.andrewaleynik.ragsystem.app.services.core.CollectionIndexService;
import com.andrewaleynik.ragsystem.domains.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CollectionCrudService collectionCrudService;

    @MockitoBean
    private CollectionIndexService collectionIndexService;

    @Test
    void createCollection_shouldReturn201() throws Exception {
        CollectionResponse response = CollectionResponse.builder().id(1L).name("docs").build();
        when(collectionCrudService.createCollection(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"docs\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("docs"));
    }

    @Test
    void getAllCollections_shouldReturn200() throws Exception {
        CollectionListResponse response = CollectionListResponse.builder()
                .count(1)
                .collections(List.of(CollectionResponse.builder().id(1L).name("docs").build()))
                .build();
        when(collectionCrudService.retrieveCollections()).thenReturn(response);

        mockMvc.perform(get("/api/v1/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void getCollectionById_shouldReturn200() throws Exception {
        CollectionListResponse response = CollectionListResponse.builder()
                .count(1)
                .collections(List.of(CollectionResponse.builder().id(1L).name("docs").build()))
                .build();
        when(collectionCrudService.retrieveCollections(any(CollectionRetrieveRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/collections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getCollectionById_shouldReturn404WhenNotFound() throws Exception {
        CollectionListResponse response = CollectionListResponse.builder()
                .count(0)
                .collections(List.of())
                .build();
        when(collectionCrudService.retrieveCollections(any(CollectionRetrieveRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/collections/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCollection_shouldReturn200() throws Exception {
        CollectionResponse response = CollectionResponse.builder().id(1L).name("updated").build();
        when(collectionCrudService.updateCollection(any(CollectionUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/collections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated"));

        verify(collectionCrudService).updateCollection(new CollectionUpdateRequest(1L, "updated"));
    }

    @Test
    void deleteCollection_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/collections/1"))
                .andExpect(status().isNoContent());

        verify(collectionCrudService).deleteCollection(new CollectionDeleteRequest(1L));
    }

    @Test
    void addDocumentsToCollection_shouldReturn200() throws Exception {
        DocumentListResponse response = new DocumentListResponse(1, 1, List.of());
        when(collectionCrudService.addDocumentsToCollection(eq(1L), eq(List.of(10L, 11L))))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/collections/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentIds\":[10,11]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void removeDocumentsFromCollection_shouldReturn200() throws Exception {
        DocumentListResponse response = new DocumentListResponse(1, 1, List.of());
        when(collectionCrudService.removeDocumentsFromCollection(eq(1L), eq(List.of(10L))))
                .thenReturn(response);

        mockMvc.perform(delete("/api/v1/collections/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentIds\":[10]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void getCollectionDocuments_shouldReturn200() throws Exception {
        DocumentListResponse response = new DocumentListResponse(0, 0, List.of());
        when(collectionCrudService.getCollectionDocuments(1L, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/collections/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void indexCollection_shouldReturn202WhenQueued() throws Exception {
        when(collectionIndexService.tryStartIndexCollection(any()))
                .thenReturn(new TaskStatusResponse(TaskStatus.QUEUED));

        mockMvc.perform(post("/api/v1/collections/1/index"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void indexCollection_shouldReturn409WhenRejected() throws Exception {
        when(collectionIndexService.tryStartIndexCollection(any()))
                .thenReturn(new TaskStatusResponse(TaskStatus.REJECTED));

        mockMvc.perform(post("/api/v1/collections/1/index"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
