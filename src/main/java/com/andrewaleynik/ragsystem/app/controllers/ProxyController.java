package com.andrewaleynik.ragsystem.app.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProxyController {
    private static final String PROXY_API_PATH = "/api/proxy/";
    //    private final AugmentService augmentService;
    private final WebClient webClient;

    @RequestMapping(PROXY_API_PATH + "**")
    public Mono<Void> proxyRequest(
            ServerHttpRequest request,
            ServerHttpResponse response,
            @RequestHeader(value = "X-Proxy-Protocol", required = false) String protocol
    ) {
        String targetHostAndPath = extractTargetHostAndPath(request);

        if (targetHostAndPath == null || targetHostAndPath.isEmpty()) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            String errorBody = "{\"error\": \"Usage: /api/proxy/{domain}/{path}\"}";
            return response.writeWith(
                    Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
            );
        }

        String usedProtocol = determineProtocol(protocol);
        String targetUrl = usedProtocol + "://" + targetHostAndPath;

        String queryString = request.getURI().getQuery();
        if (queryString != null && !queryString.isEmpty()) {
            targetUrl += "?" + queryString;
        }

        log.info("Proxying {} {} -> {}", request.getMethod(), targetHostAndPath, targetUrl);

        long startTime = System.currentTimeMillis();
        return webClient
                .method(request.getMethod())
                .uri(targetUrl)
                .headers(headers -> proxyReactiveHeaders(request.getHeaders(), headers))
                .body(request.getBody(), DataBuffer.class)
                .exchangeToMono(clientResponse -> {
                    long duration = System.currentTimeMillis() - startTime;

                    response.setStatusCode(clientResponse.statusCode());
                    response.getHeaders().putAll(clientResponse.headers().asHttpHeaders());

                    log.info("Response: {} ({} ms)", clientResponse.statusCode(), duration);

                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Proxy error: {} - {}", e.getStatusCode(), e.getMessage());
                    response.setStatusCode(e.getStatusCode());
                    response.getHeaders().putAll(e.getHeaders());
                    return response.writeWith(
                            Mono.just(response.bufferFactory().wrap(e.getResponseBodyAsByteArray()))
                    );
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Proxy error: {}", e.getMessage(), e);
                    response.setStatusCode(HttpStatus.BAD_GATEWAY);
                    String errorBody = String.format(
                            "{\"error\": \"%s\"}",
                            e.getMessage()
                    );
                    return response.writeWith(
                            Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
                    );
                });
    }

    private String extractTargetHostAndPath(ServerHttpRequest request) {
        String fullPath = request.getURI().toString();
        int proxyIndex = fullPath.indexOf(PROXY_API_PATH);

        if (proxyIndex != -1) {
            String afterProxy = fullPath.substring(proxyIndex + PROXY_API_PATH.length());

            int queryIndex = afterProxy.indexOf('?');
            if (queryIndex != -1) {
                afterProxy = afterProxy.substring(0, queryIndex);
            }

            return afterProxy;
        }

        return null;
    }

    private String determineProtocol(String headerProtocol) {
        if (headerProtocol != null && !headerProtocol.isEmpty()) {
            return headerProtocol;
        }

        return "https";
    }

    private void proxyReactiveHeaders(HttpHeaders source, HttpHeaders target) {
        List<String> excludedHeaders = List.of("host", "connection", "content-length", "transfer-encoding");

        source.forEach((key, values) -> {
            if (!excludedHeaders.contains(key.toLowerCase())) {
                target.addAll(key, values);
            }
        });
        target.add("X-Proxy-By", "ReactiveProxy");
    }
}