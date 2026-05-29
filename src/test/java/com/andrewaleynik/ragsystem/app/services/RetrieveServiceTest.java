package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.services.rag.RetrieveService;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Collection;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrieveServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private VectorStoreConfig vectorStoreConfig;

    @Mock
    private ChunkRepository chunkRepository;

    private ExVectorStore vectorStore;
    @Mock
    private VectorStore vectorStoreStore;
    @InjectMocks
    private RetrieveService retrieveService;

    private Project project1;
    private Project project2;
    private RetrieveRequest request;
    private List<Document> mockDocuments;
    private List<Chunk> mockChunkEntities;
    private com.andrewaleynik.ragsystem.data.entities.Document mockDocument;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retrieveService, "topK", 5);
        ReflectionTestUtils.setField(retrieveService, "similarityThreshold", 0.7);
        ReflectionTestUtils.setField(retrieveService, "maxResults", 10);
        ReflectionTestUtils.setField(retrieveService, "contextChunksBefore", 2);
        ReflectionTestUtils.setField(retrieveService, "contextChunksAfter", 2);

        project1 = new Project();
        project1.setId(1L);
        project1.setName("Test Project 1");
        project1.setActive(true);

        project2 = new Project();
        project2.setId(2L);
        project2.setName("Test Project 2");
        project2.setActive(true);

        Collection collection1 = new Collection();
        collection1.setId(1L);
        collection1.setName("Test Collection 1");
        collection1.setActive(true);

        request = new RetrieveRequest("test query", Collections.emptyList());

        mockDocuments = new ArrayList<>();

        Document doc1 = Document.builder()
                .id("vector-id-1")
                .text("Content 1")
                .score(0.95)
                .build();

        Document doc2 = Document.builder()
                .id("vector-id-2")
                .text("Content 2")
                .score(0.87)
                .build();

        Document doc3 = Document.builder()
                .id("vector-id-3")
                .text("Content 3")
                .score(0.76)
                .build();

        Document doc4 = Document.builder()
                .id("vector-id-4")
                .text("Content 4")
                .score(0.65)
                .build();

        Document doc5 = Document.builder()
                .id("vector-id-5")
                .text("Content 5")
                .score(0.54)
                .build();

        mockDocuments.add(doc1);
        mockDocuments.add(doc2);
        mockDocuments.add(doc3);
        mockDocuments.add(doc4);
        mockDocuments.add(doc5);

        mockChunkEntities = new ArrayList<>();
        for (int i = 0; i < mockDocuments.size(); i++) {
            Document doc = mockDocuments.get(i);
            Chunk entity = new Chunk();
            entity.setId((long) (i + 1));
            entity.setVectorId(doc.getId());
            entity.setContent(doc.getText());
            entity.setIndex(i);
            entity.setDocumentId(100L);
            entity.setHash("hash-" + i);
            entity.setSizeBytes(doc.getText().length());
            entity.setStructural(false);
            mockChunkEntities.add(entity);
        }

        mockDocument = new com.andrewaleynik.ragsystem.data.entities.Document();
        mockDocument.setId(100L);
        mockDocument.setLocalPath("/test/path/document.txt");
        mockDocument.setFileExtension("txt");

        vectorStore = new ExVectorStore("any", vectorStoreStore);
    }

    @Test
    void retrieveChunks_ShouldReturnChunksSortedByScore() {
        when(projectRepository.getAllByActive(true)).thenReturn(List.of(project1, project2));
        when(collectionRepository.getAllByActive(true)).thenReturn(List.of());
        when(vectorStoreConfig.getOrCreateVectorStore(any(Project.class))).thenReturn(vectorStore);
        when(vectorStore.vectorStore().similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);
        List<String> expectedVectorIds = List.of("vector-id-1", "vector-id-2", "vector-id-3", "vector-id-4", "vector-id-5");
        when(chunkRepository.findAllByVectorIdIn(expectedVectorIds))
                .thenReturn(mockChunkEntities);
        when(chunkRepository.findByDocumentIdAndStructural(anyLong(), anyBoolean())).thenReturn(new ArrayList<>());
        when(chunkRepository.findByDocumentIdAndStructuralAndIndexBetween(anyLong(), anyBoolean(), anyInt(),
                anyInt())).thenReturn(new ArrayList<>());
        when(documentRepository.findAllById(Set.of(100L))).thenReturn(List.of(mockDocument));

        List<Chunk> result = retrieveService.retrieveChunks(request).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result)
                .isNotNull()
                .hasSize(5);
        assertThat(result.get(0).getContent()).isEqualTo("Content 1");
        assertThat(result.get(1).getContent()).isEqualTo("Content 2");
        assertThat(result.get(2).getContent()).isEqualTo("Content 3");
        verify(projectRepository).getAllByActive(true);
        verify(collectionRepository).getAllByActive(true);
        verify(vectorStoreConfig, times(2)).getOrCreateVectorStore(any(Project.class));
        verify(vectorStore.vectorStore(), times(2)).similaritySearch(any(SearchRequest.class));
        verify(chunkRepository).findAllByVectorIdIn(expectedVectorIds);
        verify(documentRepository).findAllById(Set.of(100L));
    }

    @Test
    void retrieveChunks_ShouldLimitResultsToMaxResults() {
        ReflectionTestUtils.setField(retrieveService, "maxResults", 3);
        when(projectRepository.getAllByActive(true)).thenReturn(List.of(project1));
        when(collectionRepository.getAllByActive(true)).thenReturn(List.of());
        when(vectorStoreConfig.getOrCreateVectorStore(any(Project.class))).thenReturn(vectorStore);
        when(vectorStore.vectorStore().similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);
        List<String> expectedVectorIds = List.of("vector-id-1", "vector-id-2", "vector-id-3");
        List<Chunk> limitedEntities = mockChunkEntities.subList(0, 3);
        when(chunkRepository.findAllByVectorIdIn(expectedVectorIds))
                .thenReturn(limitedEntities);
        when(chunkRepository.findByDocumentIdAndStructural(anyLong(), anyBoolean())).thenReturn(new ArrayList<>());
        when(chunkRepository.findByDocumentIdAndStructuralAndIndexBetween(anyLong(), anyBoolean(), anyInt(),
                anyInt())).thenReturn(new ArrayList<>());
        when(documentRepository.findAllById(Set.of(100L))).thenReturn(List.of(mockDocument));

        List<Chunk> result = retrieveService.retrieveChunks(request).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("Content 1");
        assertThat(result.get(1).getContent()).isEqualTo("Content 2");
        assertThat(result.get(2).getContent()).isEqualTo("Content 3");
        verify(chunkRepository).findAllByVectorIdIn(expectedVectorIds);
        verify(documentRepository).findAllById(Set.of(100L));
    }

    @Test
    void retrieveChunks_ShouldHandleEmptyResults() {
        when(projectRepository.getAllByActive(true)).thenReturn(List.of(project1));
        when(collectionRepository.getAllByActive(true)).thenReturn(List.of());
        when(vectorStoreConfig.getOrCreateVectorStore(any(Project.class))).thenReturn(vectorStore);
        when(vectorStore.vectorStore().similaritySearch(any(SearchRequest.class)))
                .thenReturn(new ArrayList<>());

        List<Chunk> result = retrieveService.retrieveChunks(request).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result)
                .isNotNull()
                .isEmpty();
        verify(chunkRepository, never()).findAllByVectorIdIn(any());
        verify(documentRepository, never()).findAllById(any());
    }

    @Test
    void retrieveChunks_ShouldHandleEmptyChunkRepositoryResults() {
        when(projectRepository.getAllByActive(true)).thenReturn(List.of(project1));
        when(collectionRepository.getAllByActive(true)).thenReturn(List.of());
        when(vectorStoreConfig.getOrCreateVectorStore(any(Project.class))).thenReturn(vectorStore);
        when(vectorStore.vectorStore().similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);
        when(chunkRepository.findAllByVectorIdIn(anyList())).thenReturn(new ArrayList<>());

        List<Chunk> result = retrieveService.retrieveChunks(request).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result).isEmpty();
        verify(chunkRepository, never()).findByDocumentIdAndStructural(anyLong(), anyBoolean());
        verify(chunkRepository, never()).findByDocumentIdAndStructuralAndIndexBetween(anyLong(), anyBoolean(), anyInt(),
                anyInt());
        verify(documentRepository, never()).findAllById(any());
    }

    @Test
    void retrieveChunks_ShouldHandleNullQuery() {
        RetrieveRequest nullQueryRequest = new RetrieveRequest(null, Collections.emptyList());

        List<Chunk> result = retrieveService.retrieveChunks(nullQueryRequest).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result).isEmpty();
        verify(projectRepository, never()).getAllByActive(true);
        verify(collectionRepository, never()).getAllByActive(true);
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(Project.class));
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(Collection.class));
        verify(chunkRepository, never()).findAllByVectorIdIn(any());
    }

    @Test
    void retrieveChunks_ShouldHandleEmptyQuery() {
        RetrieveRequest emptyQueryRequest = new RetrieveRequest("", Collections.emptyList());

        List<Chunk> result = retrieveService.retrieveChunks(emptyQueryRequest).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result).isEmpty();
        verify(projectRepository, never()).getAllByActive(true);
        verify(collectionRepository, never()).getAllByActive(true);
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(Project.class));
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(Collection.class));
        verify(chunkRepository, never()).findAllByVectorIdIn(any());
    }

    @Test
    void retrieveChunks_ShouldHandleBlankQuery() {
        RetrieveRequest blankQueryRequest = new RetrieveRequest("   ", Collections.emptyList());

        List<Chunk> result = retrieveService.retrieveChunks(blankQueryRequest).localPathChunks().values().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(result).isEmpty();
        verify(projectRepository, never()).getAllByActive(true);
        verify(collectionRepository, never()).getAllByActive(true);
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(Project.class));
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(Collection.class));
        verify(chunkRepository, never()).findAllByVectorIdIn(any());
    }

    @Test
    void retrieveChunks_ShouldUseCorrectSearchRequestParameters() {
        when(projectRepository.getAllByActive(true)).thenReturn(List.of(project1));
        when(collectionRepository.getAllByActive(true)).thenReturn(List.of());
        when(vectorStoreConfig.getOrCreateVectorStore(any(Project.class))).thenReturn(vectorStore);
        when(vectorStore.vectorStore().similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);

        when(chunkRepository.findAllByVectorIdIn(anyList())).thenReturn(mockChunkEntities);
        when(chunkRepository.findByDocumentIdAndStructural(anyLong(), anyBoolean())).thenReturn(new ArrayList<>());
        when(chunkRepository.findByDocumentIdAndStructuralAndIndexBetween(anyLong(), anyBoolean(),
                anyInt(), anyInt())).thenReturn(new ArrayList<>());
        when(documentRepository.findAllById(Set.of(100L))).thenReturn(List.of(mockDocument));

        retrieveService.retrieveChunks(request);

        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore.vectorStore()).similaritySearch(searchRequestCaptor.capture());
        SearchRequest capturedRequest = searchRequestCaptor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo("test query");
        assertThat(capturedRequest.getTopK()).isEqualTo(5);
        assertThat(capturedRequest.getSimilarityThreshold()).isEqualTo(0.7);
    }
}