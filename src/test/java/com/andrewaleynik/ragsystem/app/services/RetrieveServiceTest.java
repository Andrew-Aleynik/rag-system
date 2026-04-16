package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.dto.project.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.services.rag.RetrieveService;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.ProjectData;
import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrieveServiceTest {

    @Mock
    private VectorStoreConfig vectorStoreConfig;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RetrieveService retrieveService;

    private ProjectDomain project1;
    private ProjectDomain project2;
    private RetrieveRequest request;
    private List<Document> mockDocuments;
    private List<ChunkJpaEntity> mockChunkEntities;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retrieveService, "topK", 5);
        ReflectionTestUtils.setField(retrieveService, "similarityThreshold", 0.7);
        ReflectionTestUtils.setField(retrieveService, "maxResults", 10);

        Long projectId1 = 1L;
        Long projectId2 = 2L;

        project1 = new ProjectFactory()
                .withId(projectId1)
                .withName("Test Project 1")
                .withUrl("url")
                .withType(ProjectType.GIT)
                .withDefaultBranch("main")
                .createDomain();

        project2 = new ProjectFactory()
                .withId(projectId2)
                .withName("Test Project 2")
                .withUrl("url")
                .withType(ProjectType.GIT)
                .withDefaultBranch("main")
                .createDomain();


        request = new RetrieveRequest("test query", List.of(project1, project2));

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
            ChunkJpaEntity entity = new ChunkJpaEntity();
            entity.setId((long) (i + 1));
            entity.setVectorId(doc.getId());
            entity.setContent(doc.getText());
            entity.setIndex(i);
            entity.setHash("hash-" + i);
            entity.setSizeBytes(doc.getText().length());
            mockChunkEntities.add(entity);
        }
    }

    @Test
    void retrieveChunks_ShouldReturnChunksSortedByScore() {
        when(vectorStoreConfig.getOrCreateVectorStore(project1)).thenReturn(vectorStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project2)).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);

        List<String> expectedVectorIds = List.of("vector-id-1", "vector-id-2", "vector-id-3", "vector-id-4", "vector-id-5");
        when(chunkRepository.findAllByVectorIdIn(expectedVectorIds))
                .thenReturn(mockChunkEntities);

        List<ChunkDomain> result = retrieveService.retrieveChunks(request);

        assertThat(result)
                .isNotNull()
                .hasSize(5);

        assertThat(result.get(0).getContent()).isEqualTo("Content 1");
        assertThat(result.get(1).getContent()).isEqualTo("Content 2");
        assertThat(result.get(2).getContent()).isEqualTo("Content 3");

        verify(vectorStoreConfig, times(2)).getOrCreateVectorStore(any(ProjectDomain.class));
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        verify(chunkRepository).findAllByVectorIdIn(expectedVectorIds);
    }

    @Test
    void retrieveChunks_ShouldLimitResultsToMaxResults() {
        ReflectionTestUtils.setField(retrieveService, "maxResults", 3);

        when(vectorStoreConfig.getOrCreateVectorStore(project1)).thenReturn(vectorStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project2)).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);

        List<String> expectedVectorIds = List.of("vector-id-1", "vector-id-2");
        List<ChunkJpaEntity> limitedEntities = mockChunkEntities.subList(0, 3);
        when(chunkRepository.findAllByVectorIdIn(expectedVectorIds))
                .thenReturn(limitedEntities);

        List<ChunkDomain> result = retrieveService.retrieveChunks(request);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("Content 1");
        assertThat(result.get(1).getContent()).isEqualTo("Content 2");
        assertThat(result.get(2).getContent()).isEqualTo("Content 3");

        verify(chunkRepository).findAllByVectorIdIn(expectedVectorIds);
    }

    @Test
    void retrieveChunks_ShouldHandleEmptyResults() {
        when(vectorStoreConfig.getOrCreateVectorStore(project1)).thenReturn(vectorStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project2)).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(new ArrayList<>());

        List<ChunkDomain> result = retrieveService.retrieveChunks(request);

        assertThat(result)
                .isNotNull()
                .isEmpty();

        verify(chunkRepository, never()).findAllByVectorIdIn(any());
    }

    @Test
    void retrieveChunks_ShouldHandleNullVectorIds() {
        when(vectorStoreConfig.getOrCreateVectorStore(project1)).thenReturn(vectorStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project2)).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);

        when(chunkRepository.findAllByVectorIdIn(anyList())).thenReturn(new ArrayList<>());

        List<ChunkDomain> result = retrieveService.retrieveChunks(request);

        assertThat(result).isEmpty();
    }

    @Test
    void retrieveChunks_ShouldHandleSingleProject() {
        RetrieveRequest singleProjectRequest = new RetrieveRequest("test query", List.of(project1));

        when(vectorStoreConfig.getOrCreateVectorStore(project1)).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);

        List<String> expectedVectorIds = List.of("vector-id-1", "vector-id-2", "vector-id-3", "vector-id-4", "vector-id-5");
        when(chunkRepository.findAllByVectorIdIn(expectedVectorIds))
                .thenReturn(mockChunkEntities);

        List<ChunkDomain> result = retrieveService.retrieveChunks(singleProjectRequest);

        assertThat(result).hasSize(5);
        verify(vectorStoreConfig, times(1)).getOrCreateVectorStore(project1);
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void retrieveChunks_ShouldHandleEmptyProjectList() {
        RetrieveRequest emptyProjectsRequest = new RetrieveRequest("test query", List.of());

        List<ChunkDomain> result = retrieveService.retrieveChunks(emptyProjectsRequest);

        assertThat(result).isEmpty();
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(ProjectData.class));
        verify(chunkRepository, never()).findAllByVectorIdIn(any());
    }

    @Test
    void retrieveChunks_ShouldHandleNullQuery() {
        RetrieveRequest nullQueryRequest = new RetrieveRequest(null, List.of(project1));

        List<ChunkDomain> result = retrieveService.retrieveChunks(nullQueryRequest);

        assertThat(result).isEmpty();
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(ProjectData.class));
    }

    @Test
    void retrieveChunks_ShouldHandleEmptyQuery() {
        RetrieveRequest emptyQueryRequest = new RetrieveRequest("", List.of(project1));

        List<ChunkDomain> result = retrieveService.retrieveChunks(emptyQueryRequest);

        assertThat(result).isEmpty();
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(ProjectData.class));
    }

    @Test
    void retrieveChunks_ShouldHandleBlankQuery() {
        RetrieveRequest blankQueryRequest = new RetrieveRequest("   ", List.of(project1));

        List<ChunkDomain> result = retrieveService.retrieveChunks(blankQueryRequest);

        assertThat(result).isEmpty();
        verify(vectorStoreConfig, never()).getOrCreateVectorStore(any(ProjectData.class));
    }

    @Test
    void retrieveChunks_ShouldUseCorrectSearchRequestParameters() {
        when(vectorStoreConfig.getOrCreateVectorStore(project1)).thenReturn(vectorStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project2)).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(mockDocuments);

        when(chunkRepository.findAllByVectorIdIn(anyList())).thenReturn(mockChunkEntities);

        retrieveService.retrieveChunks(request);

        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, times(2)).similaritySearch(searchRequestCaptor.capture());

        List<SearchRequest> capturedRequests = searchRequestCaptor.getAllValues();
        for (SearchRequest capturedRequest : capturedRequests) {
            assertThat(capturedRequest.getQuery()).isEqualTo("test query");
            assertThat(capturedRequest.getTopK()).isEqualTo(5);
            assertThat(capturedRequest.getSimilarityThreshold()).isEqualTo(0.7);
        }
    }
}
