package com.andrewaleynik.ragsystem;

import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        OpenAiChatAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class
})
public class RAGSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RAGSystemApplication.class, args);
    }

}
