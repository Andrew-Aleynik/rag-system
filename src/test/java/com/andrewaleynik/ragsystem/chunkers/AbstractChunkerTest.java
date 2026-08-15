package com.andrewaleynik.ragsystem.chunkers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractChunkerTest {

    private final AbstractChunker chunker = new DefaultChunker(100, 0.0f);

    @Test
    void computeHash_shouldBeDeterministic() {
        String hash1 = chunker.computeHash("test content");
        String hash2 = chunker.computeHash("test content");
        String hash3 = chunker.computeHash("other content");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    void readFileWithAutoEncoding_shouldReadUtf8(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("utf8.txt");
        Files.writeString(file, "Hello, мир!");

        String content = chunker.readFileWithAutoEncoding(file);

        assertThat(content).isEqualTo("Hello, мир!");
    }
}
