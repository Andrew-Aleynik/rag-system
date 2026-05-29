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
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class EmbeddingModelConfig {

    @Bean(name = "customOpenAiEmbeddingModel")
    @ConditionalOnProperty(name = "spring.ai.embedding.provider", havingValue = "openai", matchIfMissing = true)
    @Primary
    public EmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.base-url:https://routerai.ru/api/v1}") String baseUrl,
            @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}") String modelName,
            @Value("${spring.ai.openai.embedding.options.dimensions:#{null}}") Integer dimensions,
            @Value("${spring.ai.openai.embedding.options.encoding_format:float}") String encodingFormat,
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
                .baseUrl(baseUrl)
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
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}") String modelName,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaUrl,
            @Value("${spring.ai.ollama.timeout.seconds:300}") int timeoutSeconds) {

        log.info("Initializing Ollama embedding model: {} at {} (timeout: {}s)", modelName, ollamaUrl, timeoutSeconds);

        try {
            // Кастомный HttpClient с увеличенными таймаутами
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(timeoutSeconds));

            ReactorClientHttpRequestFactory requestFactory =
                    new ReactorClientHttpRequestFactory(httpClient);

            RestClient.Builder restClientBuilder = RestClient.builder()
                    .baseUrl(ollamaUrl)
                    .requestFactory(requestFactory);

            OllamaApi api = OllamaApi.builder()
                    .baseUrl(ollamaUrl)
                    .restClientBuilder(restClientBuilder)
                    .build();

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