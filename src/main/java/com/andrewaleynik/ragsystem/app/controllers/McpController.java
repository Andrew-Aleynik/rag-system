package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.response.RetrieveResponse;
import com.andrewaleynik.ragsystem.app.services.rag.RetrieveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpController {

    private final RetrieveService retrieveService;

    @McpTool(
            name = "retrieve",
            description = "Retrieve relevant code chunks from active indexed projects and collections by semantic query",
            generateOutputSchema = true
    )
    public RetrieveResponse retrieve(
            @McpToolParam(description = "Natural-language or code search query", required = true)
            String query,
            @McpToolParam(description = "Optional file extension filter, e.g. [\"java\", \"kt\"]", required = false)
            List<String> fileExtensions
    ) {
        log.info("MCP retrieve: query={}, fileExtensions={}", query, fileExtensions);
        return retrieveService.retrieveChunks(new RetrieveRequest(query, fileExtensions));
    }
}
