package com.andrewaleynik.ragsystem.app.controllers;

import com.andrewaleynik.ragsystem.app.dto.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.response.RetrieveResponse;
import com.andrewaleynik.ragsystem.app.services.rag.RetrieveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/retrieve")
@Tag(name = "Retrieve", description = "Endpoints for retrieving relevant chunks")
public class RetrieveController {

    private final RetrieveService retrieveService;

    @PostMapping
    @Operation(summary = "Retrieve chunks", description = "Retrieves relevant chunks from active projects and collections based on query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chunks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RetrieveResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RetrieveResponse> retrieveChunks(
            @Valid @RequestBody RetrieveRequest request
    ) {
        log.info("Retrieving chunks for query: {}, file types: {}", request.query(), request.fileExtensions());
        RetrieveResponse response = retrieveService.retrieveChunks(request);
        return ResponseEntity.ok(response);
    }
}
