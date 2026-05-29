package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.services.core.IndexService;
import com.andrewaleynik.ragsystem.app.services.core.QdrantVectorStoreHelper;
import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.config.ChunkerConfig;
import com.andrewaleynik.ragsystem.config.ExVectorStore;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.data.entities.Project;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.CollectionRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.data.repositories.ProjectRepository;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndexServiceTest {
    @Mock
    private EmbeddingModel embeddingModel;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CollectionRepository collectionRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ChunkRepository chunkRepository;
    @Mock
    private ChunkerConfig chunkerConfig;
    @Mock
    private VectorStoreConfig vectorStoreConfig;
    @Mock
    private QdrantVectorStoreHelper vectorStoreHelper;
    @Mock
    private ExVectorStore vectorStore;
    @Mock
    private VectorStore vectorStoreStore;
    @Mock
    private Chunker chunker;
    @InjectMocks
    private IndexService indexService;

    private Project project;
    private Document document;
    private List<Chunk> chunks;
    private List<Chunk> existingChunks;

    @BeforeEach
    void setUp() {
        Long projectId = 1L;
        Long documentId = 1L;

        project = Project.builder()
                .id(projectId)
                .url("url")
                .type(ProjectType.GIT)
                .defaultBranch("main")
                .name("Test Project")
                .build();

        document = Document.builder()
                .id(documentId)
                .localPath("/test/path/Main.java")
                .fileExtension("java")
                .build();

        chunks = new ArrayList<>();
        Chunk chunk1 = Chunk.builder()
                .documentId(documentId)
                .index(0)
                .content("public class Main {")
                .hash("hash1")
                .sizeBytes(20)
                .build();
        Chunk chunk2 = Chunk.builder()
                .documentId(documentId)
                .index(1)
                .content("    public static void main(String[] args) {")
                .hash("hash2")
                .sizeBytes(40)
                .build();
        Chunk chunk3 = Chunk.builder()
                .documentId(documentId)
                .index(2)
                .content("        System.out.println(\"Hello\");")
                .hash("hash3")
                .sizeBytes(35)
                .build();

        chunks.add(chunk1);
        chunks.add(chunk2);
        chunks.add(chunk3);

        existingChunks = new ArrayList<>();
        Chunk existingChunk = new Chunk();
        existingChunk.setDocumentId(documentId);
        existingChunk.setId(1L);
        existingChunk.setIndex(0);
        existingChunk.setHash("hash1");
        existingChunk.setContent("public class Main {");
        existingChunk.setVectorId("vector-id-1");
        existingChunks.add(existingChunk);

        EmbeddingResponse embeddingResponse = mock(EmbeddingResponse.class);
        EmbeddingResponseMetadata metadata = mock(EmbeddingResponseMetadata.class);

        when(vectorStoreConfig.getEmbeddingModel()).thenReturn(embeddingModel);
        when(embeddingModel.embedForResponse(any())).thenReturn(embeddingResponse);
        when(embeddingResponse.getMetadata()).thenReturn(metadata);
        when(metadata.getModel()).thenReturn("em-model");
        when(projectRepository.findByDocumentsContains(any())).thenReturn(project);
        when(collectionRepository.findAllByDocumentsContains(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void indexProject_ShouldIndexNewDocumentSuccessfully() throws Exception {
        project.setDocuments(List.of(document));
        when(vectorStore.vectorStore()).thenReturn(vectorStoreStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension(document.getFileExtension())).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(chunks);
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(new ArrayList<>());
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<Chunk>> saveCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<org.springframework.ai.document.Document>> vectorAddCaptor = ArgumentCaptor.forClass(List.class);

        indexService.indexNamedDocumentContainer(project);

        verify(chunkerConfig).getChunkerForExtension("java");
        verify(chunkRepository).findAllByDocumentId(document.getId());
        verify(vectorStoreStore).add(vectorAddCaptor.capture());
        verify(chunkRepository).saveAll(saveCaptor.capture());
        verify(vectorStoreHelper, never()).deleteVectorsFromVectorStore(eq(vectorStore), anyList());
        verify(chunkRepository, never()).deleteAll(anyList());

        List<Chunk> savedChunks = saveCaptor.getValue();
        assertThat(savedChunks).hasSize(3);
        assertThat(savedChunks.get(0).getIndex()).isZero();
        assertThat(savedChunks.get(0).getContent()).isEqualTo("public class Main {");
        assertThat(savedChunks.get(0).getHash()).isEqualTo("hash1");

        List<org.springframework.ai.document.Document> vectorDocuments = vectorAddCaptor.getValue();
        assertThat(vectorDocuments).hasSize(3);
        assertThat(vectorDocuments.get(0).getText()).isEqualTo("public class Main {");

        assertThat(savedChunks.get(0).getVectorId()).isEqualTo(vectorDocuments.get(0).getId());
    }

    @Test
    void indexProject_ShouldUpdateOnlyChangedChunks() throws Exception {
        // Given
        when(vectorStore.vectorStore()).thenReturn(vectorStoreStore);
        project.setDocuments(List.of(document));

        Long documentId = document.getId();

        // Modified chunks: chunk1 unchanged, chunk2 changed, chunk3 new
        List<Chunk> modifiedChunks = new ArrayList<>();
        modifiedChunks.add(Chunk.builder()
                .documentId(documentId)
                .index(0)
                .content("public class Main {")
                .hash("hash1")
                .sizeBytes(20)
                .build());
        modifiedChunks.add(Chunk.builder()
                .documentId(documentId)
                .index(1)
                .content("    public static void main(String[] args) { // Modified")
                .hash("hash2_modified")
                .sizeBytes(45)
                .build());
        modifiedChunks.add(Chunk.builder()
                .documentId(documentId)
                .index(2)
                .content("        System.out.println(\"Hello World\");")
                .hash("hash3_new")
                .sizeBytes(38)
                .build());
        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension(document.getFileExtension())).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(modifiedChunks);
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(existingChunks);
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<Chunk>> saveCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> deleteCaptor = ArgumentCaptor.forClass(List.class);

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        verify(vectorStoreStore).add(anyList());
        verify(chunkRepository).saveAll(saveCaptor.capture());
        verify(vectorStoreHelper, never()).deleteVectorsFromVectorStore(eq(vectorStore), deleteCaptor.capture());
        verify(chunkRepository, never()).deleteAll(anyList());

        // Should save 2 new chunks (chunk2 modified + chunk3 new)
        List<Chunk> savedChunks = saveCaptor.getValue();
        assertThat(savedChunks).hasSize(2);
        assertThat(savedChunks.get(0).getIndex()).isEqualTo(1);
        assertThat(savedChunks.get(0).getHash()).isEqualTo("hash2_modified");
        assertThat(savedChunks.get(1).getIndex()).isEqualTo(2);
    }

    @Test
    void indexProject_ShouldHandleMultipleDocuments() throws Exception {
        // Given
        Long doc2Id = 2L;
        Document document2 = Document.builder()
                .id(doc2Id)
                .localPath("/test/path/README.md")
                .fileExtension("md")
                .build();

        project.setDocuments(List.of(document, document2));

        when(vectorStore.vectorStore()).thenReturn(vectorStoreStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension("java")).thenReturn(chunker);
        when(chunkerConfig.getChunkerForExtension("md")).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(chunks);
        when(chunker.chunkDocument(document2)).thenReturn(List.of(
                Chunk.builder()
                        .documentId(doc2Id)
                        .index(0)
                        .content("# README")
                        .hash("readme_hash")
                        .vectorId("vector-0")
                        .sizeBytes(10)
                        .build()
        ));
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(new ArrayList<>());
        when(chunkRepository.findAllByDocumentId(document2.getId())).thenReturn(new ArrayList<>());
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<Chunk>> saveCaptor = ArgumentCaptor.forClass(List.class);

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        verify(chunker, times(2)).chunkDocument(any(Document.class));
        verify(vectorStoreStore).add(anyList());
        verify(chunkRepository, times(2)).findAllByDocumentId(any(Long.class));
        verify(chunkRepository).saveAll(saveCaptor.capture());

        List<Chunk> allSavedChunks = saveCaptor.getValue();
        assertThat(allSavedChunks).hasSize(4); // 3 + 1
    }

    @Test
    void indexProject_ShouldSetIndexedAtTimestamp() throws Exception {
        // Given
        project.setDocuments(List.of(document));
        LocalDateTime beforeTest = LocalDateTime.now();

        when(vectorStore.vectorStore()).thenReturn(vectorStoreStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension(document.getFileExtension())).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(chunks);
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(new ArrayList<>());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(documentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        Document savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getIndexedAt()).isNotNull();
        assertThat(savedDocument.getIndexedAt()).isAfterOrEqualTo(beforeTest);
    }

    @Test
    void indexProject_ShouldUseCorrectChunkerForDifferentExtensions() throws Exception {
        // Given
        var javaDoc = createDocumentWithExtension("java", "/test/Test.java");
        var pyDoc = createDocumentWithExtension("py", "/test/script.py");
        var mdDoc = createDocumentWithExtension("md", "/test/README.md");

        project.setDocuments(List.of(javaDoc, pyDoc, mdDoc));

        Chunker javaChunker = mock(Chunker.class);
        Chunker pyChunker = mock(Chunker.class);
        Chunker mdChunker = mock(Chunker.class);

        when(vectorStore.vectorStore()).thenReturn(vectorStoreStore);
        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension("java")).thenReturn(javaChunker);
        when(chunkerConfig.getChunkerForExtension("py")).thenReturn(pyChunker);
        when(chunkerConfig.getChunkerForExtension("md")).thenReturn(mdChunker);

        when(javaChunker.chunkDocument(any())).thenReturn(List.of());
        when(pyChunker.chunkDocument(any())).thenReturn(List.of());
        when(mdChunker.chunkDocument(any())).thenReturn(List.of());

        when(chunkRepository.findAllByDocumentId(any(Long.class))).thenReturn(new ArrayList<>());
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        verify(chunkerConfig).getChunkerForExtension("java");
        verify(chunkerConfig).getChunkerForExtension("py");
        verify(chunkerConfig).getChunkerForExtension("md");
        verify(javaChunker).chunkDocument(any());
        verify(pyChunker).chunkDocument(any());
        verify(mdChunker).chunkDocument(any());
    }

    private Document createDocumentWithExtension(String extension, String path) {
        Long id = 2L;
        return Document.builder()
                .id(id)
                .localPath(path)
                .fileExtension(extension)
                .build();
    }
}