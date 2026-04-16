package com.andrewaleynik.ragsystem.app.services;

import com.andrewaleynik.ragsystem.app.services.core.IndexService;
import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.config.ChunkerConfig;
import com.andrewaleynik.ragsystem.config.VectorStoreConfig;
import com.andrewaleynik.ragsystem.data.entities.ChunkJpaEntity;
import com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity;
import com.andrewaleynik.ragsystem.data.repositories.ChunkRepository;
import com.andrewaleynik.ragsystem.data.repositories.DocumentRepository;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.domains.ProjectDomain;
import com.andrewaleynik.ragsystem.domains.ProjectType;
import com.andrewaleynik.ragsystem.factories.ChunkFactory;
import com.andrewaleynik.ragsystem.factories.DocumentFactory;
import com.andrewaleynik.ragsystem.factories.ProjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ChunkerConfig chunkerConfig;

    @Mock
    private VectorStoreConfig vectorStoreConfig;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private Chunker chunker;

    @InjectMocks
    private IndexService indexService;

    private ProjectDomain project;
    private DocumentDomain document;
    private List<ChunkDomain> chunks;
    private List<ChunkJpaEntity> existingChunks;

    @BeforeEach
    void setUp() {
        Long projectId = 1L;
        Long documentId = 1L;

        project = new ProjectFactory()
                .withId(projectId)
                .withUrl("url")
                .withType(ProjectType.GIT)
                .withDefaultBranch("main")
                .withName("Test Project")
                .createDomain();

        var documentData = new DocumentJpaEntity();
        documentData.setId(documentId);
        documentData.setLocalPath("/test/path/Main.java");
        documentData.setFileExtension("java");

        document = DocumentFactory.from(documentData).createDomain();

        chunks = new ArrayList<>();
        ChunkDomain chunk1 = new ChunkFactory()
                .withIndex(0)
                .withContent("public class Main {")
                .withHash("hash1")
                .withSizeBytes(20)
                .createDomain();
        ChunkDomain chunk2 = new ChunkFactory()
                .withIndex(1)
                .withContent("    public static void main(String[] args) {")
                .withHash("hash2")
                .withSizeBytes(40)
                .createDomain();
        ChunkDomain chunk3 = new ChunkFactory()
                .withIndex(2)
                .withContent("        System.out.println(\"Hello\");")
                .withHash("hash3")
                .withSizeBytes(35)
                .createDomain();

        chunks.add(chunk1);
        chunks.add(chunk2);
        chunks.add(chunk3);

        existingChunks = new ArrayList<>();
        ChunkJpaEntity existingChunk = new ChunkJpaEntity();
        existingChunk.setId(1L);
        existingChunk.setIndex(0);
        existingChunk.setHash("hash1");
        existingChunk.setContent("public class Main {");
        existingChunk.setVectorId("vector-id-1");
        existingChunks.add(existingChunk);
    }

    @Test
    void indexProject_ShouldIndexNewDocumentSuccessfully() throws Exception {
        project.setDocuments(List.of(document));

        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension(document.getFileExtension())).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(chunks);
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(new ArrayList<>());
        when(documentRepository.save(any(DocumentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<ChunkJpaEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Document>> vectorAddCaptor = ArgumentCaptor.forClass(List.class);

        indexService.indexNamedDocumentContainer(project);

        verify(chunkerConfig).getChunkerForExtension("java");
        verify(chunkRepository).findAllByDocumentId(document.getId());
        verify(vectorStore).add(vectorAddCaptor.capture());
        verify(chunkRepository).saveAll(saveCaptor.capture());
        verify(vectorStore).delete(Collections.emptyList());
        verify(chunkRepository).deleteAllById(Collections.emptyList());

        List<ChunkJpaEntity> savedChunks = saveCaptor.getValue();
        assertThat(savedChunks).hasSize(3);
        assertThat(savedChunks.get(0).getIndex()).isZero();
        assertThat(savedChunks.get(0).getContent()).isEqualTo("public class Main {");
        assertThat(savedChunks.get(0).getHash()).isEqualTo("hash1");

        List<Document> vectorDocuments = vectorAddCaptor.getValue();
        assertThat(vectorDocuments).hasSize(3);
        assertThat(vectorDocuments.get(0).getText()).isEqualTo("public class Main {");

        assertThat(savedChunks.get(0).getVectorId()).isEqualTo(vectorDocuments.get(0).getId());
    }

    @Test
    void indexProject_ShouldUpdateOnlyChangedChunks() throws Exception {
        // Given
        project.setDocuments(List.of(document));

        // Modified chunks: chunk1 unchanged, chunk2 changed, chunk3 new
        List<ChunkDomain> modifiedChunks = new ArrayList<>();
        modifiedChunks.add(new ChunkFactory()
                .withIndex(0)
                .withContent("public class Main {")
                .withHash("hash1")
                .withSizeBytes(20)
                .createDomain());
        modifiedChunks.add(new ChunkFactory()
                .withIndex(1)
                .withContent("    public static void main(String[] args) { // Modified")
                .withHash("hash2_modified")
                .withSizeBytes(45)
                .createDomain());
        modifiedChunks.add(new ChunkFactory()
                .withIndex(2)
                .withContent("        System.out.println(\"Hello World\");")
                .withHash("hash3_new")
                .withSizeBytes(38)
                .createDomain());

        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension(document.getFileExtension())).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(modifiedChunks);
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(existingChunks);
        when(documentRepository.save(any(DocumentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<ChunkJpaEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> deleteCaptor = ArgumentCaptor.forClass(List.class);

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        verify(vectorStore).add(anyList());
        verify(chunkRepository).saveAll(saveCaptor.capture());
        verify(vectorStore).delete(deleteCaptor.capture());
        verify(chunkRepository).deleteAllById(anyList());

        // Should save 2 new chunks (chunk2 modified + chunk3 new)
        List<ChunkJpaEntity> savedChunks = saveCaptor.getValue();
        assertThat(savedChunks).hasSize(2);
        assertThat(savedChunks.get(0).getIndex()).isEqualTo(1);
        assertThat(savedChunks.get(0).getHash()).isEqualTo("hash2_modified");
        assertThat(savedChunks.get(1).getIndex()).isEqualTo(2);
    }

    @Test
    void indexProject_ShouldHandleMultipleDocuments() throws Exception {
        // Given
        Long doc2Id = 2L;
        var documentData2 = new com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity();
        documentData2.setId(doc2Id);
        documentData2.setLocalPath("/test/path/README.md");
        documentData2.setFileExtension("md");
        DocumentDomain document2 = DocumentFactory.from(documentData2).createDomain();

        project.setDocuments(List.of(document, document2));

        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension("java")).thenReturn(chunker);
        when(chunkerConfig.getChunkerForExtension("md")).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(chunks);
        when(chunker.chunkDocument(document2)).thenReturn(List.of(
                new ChunkFactory()
                        .withIndex(0)
                        .withContent("# README")
                        .withHash("readme_hash")
                        .withSizeBytes(10)
                        .createDomain()
        ));
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(new ArrayList<>());
        when(chunkRepository.findAllByDocumentId(document2.getId())).thenReturn(new ArrayList<>());
        when(documentRepository.save(any(DocumentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<List<ChunkJpaEntity>> saveCaptor = ArgumentCaptor.forClass(List.class);

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        verify(chunker, times(2)).chunkDocument(any(DocumentDomain.class));
        verify(vectorStore).add(anyList());
        verify(chunkRepository, times(2)).findAllByDocumentId(any(Long.class));
        verify(chunkRepository).saveAll(saveCaptor.capture());

        List<ChunkJpaEntity> allSavedChunks = saveCaptor.getValue();
        assertThat(allSavedChunks).hasSize(4); // 3 + 1
    }

    @Test
    void indexProject_ShouldSetIndexedAtTimestamp() throws Exception {
        // Given
        project.setDocuments(List.of(document));
        LocalDateTime beforeTest = LocalDateTime.now();

        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension(document.getFileExtension())).thenReturn(chunker);
        when(chunker.chunkDocument(document)).thenReturn(chunks);
        when(chunkRepository.findAllByDocumentId(document.getId())).thenReturn(new ArrayList<>());

        ArgumentCaptor<DocumentJpaEntity> documentCaptor = ArgumentCaptor.forClass(DocumentJpaEntity.class);
        when(documentRepository.save(documentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // When
        indexService.indexNamedDocumentContainer(project);

        // Then
        DocumentJpaEntity savedDocument = documentCaptor.getValue();
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

        when(vectorStoreConfig.getOrCreateVectorStore(project)).thenReturn(vectorStore);
        when(chunkerConfig.getChunkerForExtension("java")).thenReturn(javaChunker);
        when(chunkerConfig.getChunkerForExtension("py")).thenReturn(pyChunker);
        when(chunkerConfig.getChunkerForExtension("md")).thenReturn(mdChunker);

        when(javaChunker.chunkDocument(any())).thenReturn(List.of());
        when(pyChunker.chunkDocument(any())).thenReturn(List.of());
        when(mdChunker.chunkDocument(any())).thenReturn(List.of());

        when(chunkRepository.findAllByDocumentId(any(Long.class))).thenReturn(new ArrayList<>());
        when(documentRepository.save(any(DocumentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

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

    private DocumentDomain createDocumentWithExtension(String extension, String path) {
        Long id = 2L;
        var documentData = new com.andrewaleynik.ragsystem.data.entities.DocumentJpaEntity();
        documentData.setId(id);
        documentData.setLocalPath(path);
        documentData.setFileExtension(extension);
        return DocumentFactory.from(documentData).createDomain();
    }
}