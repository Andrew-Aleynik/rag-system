package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.response.RetrieveResponse;
import com.andrewaleynik.ragsystem.app.services.rag.RetrieveService;
import com.andrewaleynik.ragsystem.data.entities.Chunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RetrieveController.class)
class RetrieveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RetrieveService retrieveService;

    @Test
    void retrieveChunks_shouldReturn200() throws Exception {
        Chunk chunk = Chunk.builder()
                .content("public class Main {}")
                .index(0)
                .build();
        RetrieveResponse response = new RetrieveResponse(
                Map.of("/path/Main.java", List.of(chunk))
        );
        when(retrieveService.retrieveChunks(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"How does Main work?","fileExtensions":["java"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localPathChunks['/path/Main.java'][0].content")
                        .value("public class Main {}"));
    }
}
