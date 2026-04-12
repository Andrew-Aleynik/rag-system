package com.andrewaleynik.ragsystem.config;

import com.andrewaleynik.ragsystem.chunkers.AstChunker;
import com.andrewaleynik.ragsystem.chunkers.Chunker;
import com.andrewaleynik.ragsystem.chunkers.DefaultChunker;
import com.andrewaleynik.universalparser.Analyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkerConfig {
    private final AnalyzerConfig analyzerConfig;
    @Value("${chunker.max_size:700}")
    private int maxSize;
    @Value("${chunker.min_payload_size:200}")
    private int minPayloadSize;
    @Value("${chunker.overlap:0.2}")
    private float overlap;

    public Chunker getChunkerForExtension(String extension) {
        Analyzer analyzer = analyzerConfig.getAnalyzerForExtension(extension);
        if (analyzer == null) {
            log.warn("No analyzer found for extension: {}", extension);
            return new DefaultChunker(maxSize, overlap);
        }
        return new AstChunker(analyzer, maxSize, minPayloadSize, overlap);
    }

    @Bean
    @Primary
    public Chunker defaultChunker() {
        return new DefaultChunker(maxSize, overlap);
    }
}
