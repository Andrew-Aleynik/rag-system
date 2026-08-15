package com.andrewaleynik.ragsystem.app.services.rag;

import com.andrewaleynik.ragsystem.app.dto.request.AugmentRequest;
import com.andrewaleynik.ragsystem.app.dto.response.AugmentResponse;
import com.andrewaleynik.ragsystem.app.dto.response.RetrieveResponse;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AugmentServiceTest {

    @Mock
    private RetrieveService retrieveService;

    @InjectMocks
    private AugmentService augmentService;

    @Test
    void augment_shouldPrependRagContextToLastUserMessage() {
        Chunk chunk = Chunk.builder().content("class Main {}").build();
        when(retrieveService.retrieveChunks(any())).thenReturn(
                new RetrieveResponse(Map.of("/src/Main.java", List.of(chunk)))
        );

        String body = """
                {"messages":[{"role":"system","content":"You are helpful"},{"role":"user","content":"What is Main?"}]}
                """;
        AugmentResponse response = augmentService.augment(new AugmentRequest(body));

        assertThat(response.augmentedRequestBody()).contains("Relevant context:");
        assertThat(response.augmentedRequestBody()).contains("/src/Main.java");
        assertThat(response.augmentedRequestBody()).contains("class Main {}");
        assertThat(response.augmentedRequestBody()).contains("What is Main?");
        verify(retrieveService).retrieveChunks(any());
    }

    @Test
    void augment_shouldReturnOriginalWhenNoUserMessage() {
        String body = """
                {"messages":[{"role":"system","content":"You are helpful"}]}
                """;

        AugmentResponse response = augmentService.augment(new AugmentRequest(body));

        assertThat(response.augmentedRequestBody()).isEqualTo(body);
    }

    @Test
    void augment_shouldReturnOriginalOnInvalidJson() {
        String body = "not-json";

        AugmentResponse response = augmentService.augment(new AugmentRequest(body));

        assertThat(response.augmentedRequestBody()).isEqualTo(body);
    }

    @Test
    void augment_shouldUseLastUserMessageWhenMultiple() {
        when(retrieveService.retrieveChunks(any())).thenReturn(
                new RetrieveResponse(Map.of())
        );

        String body = """
                {"messages":[{"role":"user","content":"first"},{"role":"assistant","content":"ok"},{"role":"user","content":"second"}]}
                """;
        AugmentResponse response = augmentService.augment(new AugmentRequest(body));

        assertThat(response.augmentedRequestBody()).contains("second");
        assertThat(response.augmentedRequestBody()).doesNotContain("Based on the above context, please answer the following question:\nfirst");
    }
}
