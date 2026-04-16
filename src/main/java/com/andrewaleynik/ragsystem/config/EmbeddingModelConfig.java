package com.andrewaleynik.ragsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class EmbeddingModelConfig {

    @Bean(name = "customOpenAiEmbeddingModel")
    @ConditionalOnProperty(name = "spring.ai.embedding.provider", havingValue = "openai", matchIfMissing = true)
    @Primary
    public EmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.model.embedding:text-embedding-3-small}") String modelName,
            @Value("${spring.ai.openai.dimensions:1536}") int dimensions,
            @Value("${spring.ai.openai.encoding_format:float}") String encodingFormat,
            @Value("${spring.ai.openai.api-key:sk-default-key}") String apiKey) {

        String resolvedApiKey = System.getenv("OPENAI_API_KEY");
        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            resolvedApiKey = apiKey;
        }
        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is required. Set OPENAI_API_KEY environment variable " +
                    "or openai.api-key property");
        }

        log.info("Initializing OpenAI embedding model: {} (dimensions: {})", modelName, dimensions);

        OpenAiApi api = OpenAiApi.builder()
                .apiKey(resolvedApiKey)
                .build();

        return new OpenAiEmbeddingModel(
                api,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(modelName)
                        .encodingFormat(encodingFormat)
                        .dimensions(dimensions)
                        .build()
        );
    }

    @Bean(name = "customOllamaEmbeddingModel")
    @ConditionalOnProperty(name = "spring.ai.embedding.provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel(
            @Value("${ollama.model:nomic-embed-text}") String modelName,
            @Value("${ollama.url:http://localhost:11434}") String ollamaUrl) {

        log.info("Initializing Ollama embedding model: {} at {}", modelName, ollamaUrl);

        try {
            OllamaApi api = OllamaApi.builder().baseUrl(ollamaUrl).build();
            return OllamaEmbeddingModel.builder()
                    .ollamaApi(api)
                    .defaultOptions(OllamaEmbeddingOptions.builder()
                            .model(modelName)
                            .build()
                    )
                    .build();
        } catch (Exception e) {
            log.error("Failed to connect to Ollama at {}", ollamaUrl, e);
            throw new RuntimeException("Ollama is not available: " + e.getMessage(), e);
        }
    }
}