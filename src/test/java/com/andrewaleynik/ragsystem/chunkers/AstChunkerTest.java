package com.andrewaleynik.ragsystem.chunkers;

import com.andrewaleynik.ragsystem.config.AnalyzerConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AstChunkerTest {

    private AstChunker chunker;
    private final AnalyzerConfig analyzerConfig = new AnalyzerConfig();

    @BeforeEach
    void setUp() {
        chunker = new AstChunker(analyzerConfig.createJavaFileAnalyzer(), 700, 200, 0.1f);
    }

    @Test
    void chunkDocument_shouldProduceStructuralAndLeafChunks() throws Exception {
        Path file = Path.of(getClass().getClassLoader().getResource("samples/sample2.java").toURI());
        Document document = Document.builder().id(1L).localPath(file.toString()).build();

        List<Chunk> chunks = chunker.chunkDocument(document);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().anyMatch(Chunk::getStructural)).isTrue();
        assertThat(chunks.stream().anyMatch(c -> !c.getStructural())).isTrue();
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getIndex()).isEqualTo(i);
            assertThat(chunks.get(i).getDocumentId()).isEqualTo(1L);
            assertThat(chunks.get(i).getHash()).isNotBlank();
            assertThat(chunks.get(i).getContent()).isNotBlank();
        }
    }

    @Test
    void chunkDocument_shouldChunkJavaFileWithMultipleMethods() throws Exception {
        Path file = Path.of(getClass().getClassLoader().getResource("samples/sample4.java").toURI());
        Document document = Document.builder().id(2L).localPath(file.toString()).build();

        List<Chunk> chunks = chunker.chunkDocument(document);

        assertThat(chunks).hasSizeGreaterThan(2);
    }

    @Test
    void chunkDocument_shouldThrowWhenStructureTooComplex() throws Exception {
        Path file = Path.of(getClass().getClassLoader().getResource("samples/sample4.java").toURI());
        Document document = Document.builder().id(3L).localPath(file.toString()).build();
        AstChunker strictChunker = new AstChunker(
                analyzerConfig.createJavaFileAnalyzer(), 50, 40, 0.0f
        );

        assertThatThrownBy(() -> strictChunker.chunkDocument(document))
                .isInstanceOf(ChunkingException.class)
                .hasMessageContaining("too complex");
    }
}
