package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.request.AugmentRequest;
import com.andrewaleynik.ragsystem.app.dto.response.AugmentResponse;
import com.andrewaleynik.ragsystem.app.services.rag.AugmentService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProxyController.class)
@Import(ProxyControllerTest.ProxyTestConfig.class)
class ProxyControllerTest {

    private static HttpServer upstream;
    private static int upstreamPort;
    private static final AtomicReference<String> lastUpstreamBody = new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AugmentService augmentService;

    @BeforeAll
    static void startUpstream() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/v1/chat/completions", exchange -> {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            lastUpstreamBody.set(new String(requestBytes, StandardCharsets.UTF_8));
            byte[] responseBytes = "{\"id\":\"chatcmpl-1\",\"choices\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        upstream.start();
        upstreamPort = upstream.getAddress().getPort();
    }

    @AfterAll
    static void stopUpstream() {
        if (upstream != null) {
            upstream.stop(0);
        }
    }

    @Test
    void proxy_shouldAugmentBodyAndForwardToUpstream() throws Exception {
        String original = """
                {"messages":[{"role":"user","content":"How does auth work?"}]}
                """;
        String augmented = """
                {"messages":[{"role":"user","content":"Relevant context:\\nclass Auth {}\\n\\nHow does auth work?"}]}
                """;
        when(augmentService.augment(any(AugmentRequest.class)))
                .thenReturn(new AugmentResponse(augmented));

        mockMvc.perform(post("/api/v1/proxy/127.0.0.1:" + upstreamPort + "/v1/chat/completions")
                        .header("X-Proxy-Protocol", "http")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(original))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"chatcmpl-1\",\"choices\":[]}"));

        assertThat(lastUpstreamBody.get()).isEqualTo(augmented);
    }

    @Test
    void proxy_shouldReturnBadRequestWhenTargetMissing() throws Exception {
        mockMvc.perform(post("/api/v1/proxy/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Usage: /api/v1/proxy/{host}/{path}\"}"));
    }

    @TestConfiguration
    static class ProxyTestConfig {
        @Bean
        RestClient proxyRestClient() {
            return RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory())
                    .build();
        }
    }
}
