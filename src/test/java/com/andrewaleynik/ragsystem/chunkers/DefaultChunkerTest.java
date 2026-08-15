package com.andrewaleynik.ragsystem.chunkers;

import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultChunkerTest {

    private DefaultChunker chunker;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        chunker = new DefaultChunker(20, 0.0f);
    }

    @Test
    void chunkDocument_shouldReturnEmptyForEmptyFile() throws Exception {
        Path file = tempDir.resolve("empty.txt");
        Files.writeString(file, "");

        Document document = Document.builder().id(1L).localPath(file.toString()).build();

        List<Chunk> chunks = chunker.chunkDocument(document);

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkDocument_shouldReturnSingleChunkForShortContent() throws Exception {
        Path file = tempDir.resolve("short.txt");
        Files.writeString(file, "hello world");

        Document document = Document.builder().id(1L).localPath(file.toString()).build();

        List<Chunk> chunks = chunker.chunkDocument(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("hello world");
        assertThat(chunks.get(0).getIndex()).isZero();
        assertThat(chunks.get(0).getDocumentId()).isEqualTo(1L);
        assertThat(chunks.get(0).getHash()).isNotBlank();
    }

    @Test
    void chunkDocument_shouldSplitLongContent() throws Exception {
        Path file = tempDir.resolve("long.txt");
        String content = "word ".repeat(30);
        Files.writeString(file, content);

        Document document = Document.builder().id(2L).localPath(file.toString()).build();

        List<Chunk> chunks = chunker.chunkDocument(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getIndex()).isEqualTo(i);
            assertThat(chunks.get(i).getSizeBytes()).isLessThanOrEqualTo(content.length());
        }
    }

    @Test
    void chunkDocument_shouldRespectOverlap() throws Exception {
        DefaultChunker overlappingChunker = new DefaultChunker(15, 0.5f);
        Path file = tempDir.resolve("overlap.txt");
        Files.writeString(file, "abcdefghijklmnopqrstuvwxyz");

        Document document = Document.builder().id(3L).localPath(file.toString()).build();

        List<Chunk> chunks = overlappingChunker.chunkDocument(document);

        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    void chunkDocument_shouldReadExistingTestResource() throws Exception {
        DefaultChunker largeChunker = new DefaultChunker(700, 0.0f);
        Path file = Path.of(getClass().getClassLoader().getResource("files/script.py").toURI());

        Document document = Document.builder().id(4L).localPath(file.toString()).build();

        List<Chunk> chunks = largeChunker.chunkDocument(document);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getContent()).contains("python");
    }
}
