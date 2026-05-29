package com.andrewaleynik.ragsystem;

import com.andrewaleynik.ragsystem.chunkers.AstChunker;
import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.chunkers.DefaultChunker;
import com.andrewaleynik.ragsystem.config.AnalyzerConfig;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import com.andrewaleynik.ragsystem.data.entities.Document;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

@Disabled("Manual test")
class ChunkerTest {
    private static final AnalyzerConfig analyzerConfig = new AnalyzerConfig();

    @ParameterizedTest
    @MethodSource("chunkersSamples")
    void testChunker(Chunker chunker, URL sampleResource) throws IOException {
        Document document = Document.builder()
                .id(1L)
                .localPath(sampleResource.getPath())
                .build();
        try {
            List<Chunk> chunks = chunker.chunkDocument(document);
            for (Chunk chunk : chunks) {
                System.out.println("=".repeat(70));
                System.out.println(chunk.getIndex());
                System.out.println(chunk.getStructural());
                System.out.println(chunk.getHash());
                System.out.println(chunk.getContent());
                System.out.println("=".repeat(70));
            }
        } catch (ChunkingException e) {
            System.out.println("Error during chunking: " + e);
        }
    }

    static Stream<Arguments> chunkersSamples() {
        return Stream.of(
                Arguments.of(new AstChunker(analyzerConfig.createJavaFileAnalyzer(), 5000, 700, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample1.java")),
                Arguments.of(new AstChunker(analyzerConfig.createJavaFileAnalyzer(), 700, 200, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample2.java")),
                Arguments.of(new AstChunker(analyzerConfig.createJavaFileAnalyzer(), 700, 200, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample3.java")),
                Arguments.of(new AstChunker(analyzerConfig.createJavaFileAnalyzer(), 700, 200, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample4.java")),
                Arguments.of(new DefaultChunker(700, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample1.java")),
                Arguments.of(new DefaultChunker(700, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample2.java")),
                Arguments.of(new DefaultChunker(700, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample3.java")),
                Arguments.of(new DefaultChunker(700, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample4.java"))
        );
    }
}
