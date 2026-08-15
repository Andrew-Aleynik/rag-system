package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.request.collection.*;
import com.andrewaleynik.ragsystem.app.dto.response.CollectionListResponse;
import com.andrewaleynik.ragsystem.app.dto.response.CollectionResponse;
import com.andrewaleynik.ragsystem.app.dto.response.DocumentListResponse;
import com.andrewaleynik.ragsystem.app.services.core.VectorStoreHelper;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionCrudServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ChunkRepository chunkRepository;
    @Mock
    private VectorStoreConfig vectorStoreConfig;
    @Mock
    private VectorStoreHelper vectorStoreHelper;
    @Mock
    private ExVectorStore exVectorStore;
    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private CollectionCrudService collectionCrudService;

    private Collection collection;
    private Document document;

    @BeforeEach
    void setUp() {
        collection = Collection.builder()
                .id(1L)
                .name("test-collection")
                .active(true)
                .documents(new ArrayList<>())
                .build();

        document = Document.builder()
                .id(10L)
                .projectId(5L)
                .localPath("/tmp/doc.java")
                .fileName("doc.java")
                .fileExtension("java")
                .fileHash("abc")
                .build();
    }

    @Test
    void createCollection_shouldSaveAndReturnResponse() {
        when(collectionRepository.save(any(Collection.class))).thenAnswer(inv -> {
            Collection c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CollectionResponse response = collectionCrudService.createCollection(
                new CollectionCreateRequest("new-collection")
        );

        assertThat(response.name()).isEqualTo("new-collection");
        verify(collectionRepository).save(any(Collection.class));
    }

    @Test
    void retrieveCollections_shouldReturnAll() {
        when(collectionRepository.findAll()).thenReturn(List.of(collection));

        CollectionListResponse response = collectionCrudService.retrieveCollections();

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.collections().get(0).name()).isEqualTo("test-collection");
    }

    @Test
    void retrieveCollectionsByIds_shouldReturnMatching() {
        when(collectionRepository.findAllById(List.of(1L))).thenReturn(List.of(collection));

        CollectionListResponse response = collectionCrudService.retrieveCollections(
                new CollectionRetrieveRequest(List.of(1L))
        );

        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void updateCollection_shouldUpdateName() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        when(collectionRepository.save(collection)).thenReturn(collection);

        CollectionResponse response = collectionCrudService.updateCollection(
                new CollectionUpdateRequest(1L, "renamed")
        );

        assertThat(response.name()).isEqualTo("renamed");
        assertThat(collection.getName()).isEqualTo("renamed");
    }

    @Test
    void updateCollection_shouldThrowWhenNotFound() {
        when(collectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionCrudService.updateCollection(
                new CollectionUpdateRequest(99L, "x")
        )).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteCollection_shouldDeleteVectorStoreAndEntity() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));

        collectionCrudService.deleteCollection(new CollectionDeleteRequest(1L));

        verify(vectorStoreConfig).deleteVectorStore(collection);
        verify(collectionRepository).deleteById(1L);
    }

    @Test
    void activateCollection_shouldSetActiveTrue() {
        collection.setActive(false);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));

        collectionCrudService.activateCollection(new CollectionActivateRequest(1L));

        assertThat(collection.getActive()).isTrue();
        verify(collectionRepository).save(collection);
    }

    @Test
    void deactivateCollection_shouldSetActiveFalse() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));

        collectionCrudService.deactivateCollection(new CollectionDeactivateRequest(1L));

        assertThat(collection.getActive()).isFalse();
        verify(collectionRepository).save(collection);
    }

    @Test
    void addDocumentsToCollection_shouldAddNewDocuments() {
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));
        when(collectionRepository.save(collection)).thenReturn(collection);
        when(vectorStoreConfig.getOrCreateVectorStore(collection)).thenReturn(exVectorStore);
        when(chunkRepository.findAllByDocumentId(10L)).thenReturn(List.of(
                Chunk.builder().vectorId("vec-1").build()
        ));
        Project project = Project.builder().id(5L).build();
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(exVectorStore);
        when(vectorStoreHelper.fetchVectorsFromVectorStore(eq(exVectorStore), anyList()))
                .thenReturn(List.of());

        DocumentListResponse response = collectionCrudService.addDocumentsToCollection(1L, List.of(10L));

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(collection.getDocuments()).hasSize(1);
        verify(vectorStoreHelper).writeVectorsToVectorStore(eq(exVectorStore), anyList());
    }

    @Test
    void addDocumentsToCollection_shouldReturnEmptyWhenAlreadyPresent() {
        collection.addDocument(document);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));

        DocumentListResponse response = collectionCrudService.addDocumentsToCollection(1L, List.of(10L));

        assertThat(response.totalCount()).isZero();
        verify(collectionRepository, never()).save(any());
    }

    @Test
    void removeDocumentsFromCollection_shouldRemoveAndDeleteVectors() {
        collection.addDocument(document);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        when(documentRepository.findAllById(List.of(10L))).thenReturn(List.of(document));
        when(collectionRepository.save(collection)).thenReturn(collection);
        when(vectorStoreConfig.getOrCreateVectorStore(collection)).thenReturn(exVectorStore);
        when(exVectorStore.vectorStore()).thenReturn(vectorStore);
        when(chunkRepository.findAllByDocumentId(10L)).thenReturn(List.of(
                Chunk.builder().vectorId("vec-1").build()
        ));

        DocumentListResponse response = collectionCrudService.removeDocumentsFromCollection(1L, List.of(10L));

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(collection.getDocuments()).isEmpty();
        verify(vectorStore).delete(List.of("vec-1"));
    }

    @Test
    void getCollectionDocuments_shouldReturnPaginated() {
        collection.addDocument(document);
        when(collectionRepository.findById(1L)).thenReturn(Optional.of(collection));
        Page<Document> page = new PageImpl<>(List.of(document));
        when(documentRepository.findByCollectionId(eq(1L), any(Pageable.class))).thenReturn(page);

        DocumentListResponse response = collectionCrudService.getCollectionDocuments(1L, 0, 20);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.documents()).hasSize(1);
    }
}
