package com.andrewaleynik.ragsystem;

import com.andrewaleynik.ragsystem.analyzers.JavaAnalyzerConfig;
import com.andrewaleynik.ragsystem.chunkers.AstChunker;
import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.chunkers.DefaultChunker;
import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.factories.DocumentFactory;
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
    @ParameterizedTest
    @MethodSource("chunkersSamples")
    void testChunker(Chunker chunker, URL sampleResource) throws IOException {
        DocumentDomain document = new DocumentFactory()
                .withId(1L)
                .withLocalPath(sampleResource.getPath())
                .createDomain();
        List<ChunkDomain> chunks = chunker.chunkDocument(document);
        for (ChunkDomain chunk : chunks) {
            System.out.println("=".repeat(70));
            System.out.println(chunk.getIndex());
            System.out.println(chunk.getStructural());
            System.out.println(chunk.getHash());
            System.out.println(chunk.getContent());
            System.out.println("=".repeat(70));
        }
    }

    static Stream<Arguments> chunkersSamples() {
        return Stream.of(
                Arguments.of(new AstChunker(JavaAnalyzerConfig.createJavaFileAnalyzer(), 700, 200, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample1.java")),
                Arguments.of(new DefaultChunker(700, 0.1f),
                        ChunkerTest.class.getClassLoader().getResource("samples/sample1.java"))
        );
    }
}
