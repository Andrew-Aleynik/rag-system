package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.request.AugmentRequest;
import com.andrewaleynik.ragsystem.app.dto.response.AugmentResponse;
import com.andrewaleynik.ragsystem.app.services.rag.AugmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/proxy")
@Tag(name = "Proxy", description = "LLM proxy with automatic RAG context injection")
public class ProxyController {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length",
            "content-encoding",
            "x-proxy-protocol"
    );

    private final AugmentService augmentService;
    private final RestClient proxyRestClient;

    @RequestMapping("/{*target}")
    @Operation(
            summary = "Proxy request to LLM with RAG augmentation",
            description = "Forwards the request to {protocol}://{target}. "
                    + "Chat-completions bodies are augmented with retrieved context. "
                    + "Set X-Proxy-Protocol header to http or https (default: https)."
    )
    public void proxyRequest(
            @Parameter(description = "Target host and path, e.g. api.openai.com/v1/chat/completions")
            @PathVariable("target") String target,
            @RequestHeader(value = "X-Proxy-Protocol", required = false) String protocol,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String targetHostAndPath = normalizeTarget(target);
        if (!StringUtils.hasText(targetHostAndPath)) {
            writeError(response, HttpStatus.BAD_REQUEST, "Usage: /api/v1/proxy/{host}/{path}");
            return;
        }

        String targetUrl = buildTargetUrl(protocol, targetHostAndPath, request.getQueryString());
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        byte[] originalBody = StreamUtils.copyToByteArray(request.getInputStream());
        String originalBodyJson = new String(originalBody, StandardCharsets.UTF_8);
        String modifiedBody = modify(originalBodyJson);

        log.info("Proxying {} {} -> {}", method, request.getRequestURI(), targetUrl);
        long startTime = System.currentTimeMillis();

        try {
            RestClient.RequestBodySpec requestSpec = proxyRestClient
                    .method(method)
                    .uri(targetUrl)
                    .headers(headers -> copyRequestHeaders(request, headers, modifiedBody));

            if (shouldSendBody(method, modifiedBody)) {
                requestSpec.body(modifiedBody.getBytes(StandardCharsets.UTF_8));
            }

            requestSpec.exchange((clientRequest, clientResponse) -> {
                response.setStatus(clientResponse.getStatusCode().value());
                copyResponseHeaders(clientResponse.getHeaders(), response);

                InputStream bodyStream = clientResponse.getBody();
                if (bodyStream != null) {
                    StreamUtils.copy(bodyStream, response.getOutputStream());
                }
                response.flushBuffer();

                long duration = System.currentTimeMillis() - startTime;
                log.info("Proxy response: {} ({} ms)", clientResponse.getStatusCode(), duration);
                return null;
            });
        } catch (RestClientResponseException e) {
            log.error("Proxy upstream error: {} - {}", e.getStatusCode(), e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(e.getStatusCode().value());
                e.getResponseHeaders().forEach((name, values) -> {
                    if (!isHopByHop(name)) {
                        values.forEach(value -> response.addHeader(name, value));
                    }
                });
                byte[] errorBody = e.getResponseBodyAsByteArray();
                if (errorBody.length > 0) {
                    response.getOutputStream().write(errorBody);
                }
            }
        } catch (Exception e) {
            log.error("Proxy error: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                writeError(response, HttpStatus.BAD_GATEWAY, e.getMessage());
            }
        }
    }

    private boolean shouldSendBody(HttpMethod method, String body) {
        return StringUtils.hasText(body)
                && method != HttpMethod.GET
                && method != HttpMethod.HEAD
                && method != HttpMethod.OPTIONS
                && method != HttpMethod.TRACE;
    }

    private String modify(String originalBodyJson) {
        if (!StringUtils.hasText(originalBodyJson)) {
            return originalBodyJson;
        }
        AugmentResponse augmentResponse = augmentService.augment(new AugmentRequest(originalBodyJson));
        return augmentResponse.augmentedRequestBody();
    }

    private String normalizeTarget(String target) {
        if (target == null) {
            return null;
        }
        String normalized = target.startsWith("/") ? target.substring(1) : target;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        return normalized;
    }

    private String buildTargetUrl(String protocolHeader, String targetHostAndPath, String queryString) {
        String protocol = determineProtocol(protocolHeader);
        StringBuilder url = new StringBuilder(protocol)
                .append("://")
                .append(targetHostAndPath);
        if (StringUtils.hasText(queryString)) {
            url.append('?').append(queryString);
        }
        return url.toString();
    }

    private String determineProtocol(String headerProtocol) {
        if (!StringUtils.hasText(headerProtocol)) {
            return "https";
        }
        String normalized = headerProtocol.trim().toLowerCase(Locale.ROOT);
        if ("http".equals(normalized) || "https".equals(normalized)) {
            return normalized;
        }
        return "https";
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpHeaders target, String body) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (isHopByHop(name)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                target.add(name, values.nextElement());
            }
        }
        if (StringUtils.hasText(body)) {
            target.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            target.setContentLength(body.getBytes(StandardCharsets.UTF_8).length);
        }
        target.set("X-Proxy-By", "RAGSystem");
    }

    private void copyResponseHeaders(HttpHeaders source, HttpServletResponse response) {
        source.forEach((name, values) -> {
            if (!isHopByHop(name)) {
                values.forEach(value -> response.addHeader(name, value));
            }
        });
    }

    private boolean isHopByHop(String headerName) {
        return headerName != null && HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safeMessage = message == null ? "Unknown error" : message.replace("\"", "'");
        String errorBody = "{\"error\":\"" + safeMessage + "\"}";
        response.getOutputStream().write(errorBody.getBytes(StandardCharsets.UTF_8));
    }
}
