package com.andrewaleynik.ragsystem.app.services.rag;

import com.andrewaleynik.ragsystem.app.dto.project.request.AugmentRequest;
import com.andrewaleynik.ragsystem.app.dto.project.request.RetrieveRequest;
import com.andrewaleynik.ragsystem.app.dto.project.response.AugmentResponse;
import com.andrewaleynik.ragsystem.app.dto.project.response.RetrieveResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AugmentService {
    private final RetrieveService retrieveService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MESSAGES = "messages";
    private static final String ROLE = "role";
    private static final String USER = "user";
    private static final String CONTENT = "content";

    public AugmentResponse augment(AugmentRequest request) {
        String originalBodyJson = request.requestBody();

        try {
            JsonNode rootNode = objectMapper.readTree(originalBodyJson);

            if (rootNode.has(MESSAGES) && rootNode.get(MESSAGES).isArray()) {
                ArrayNode messages = (ArrayNode) rootNode.get(MESSAGES);

                String userQuery = null;
                int userMessageIndex = -1;

                for (int i = 0; i < messages.size(); i++) {
                    JsonNode message = messages.get(i);
                    if (message.has(ROLE) && USER.equals(message.get(ROLE).asText())) {
                        userQuery = message.get(CONTENT).asText();
                        userMessageIndex = i;
                    }
                }

                if (userQuery != null) {
                    RetrieveRequest retrieveRequest = new RetrieveRequest(userQuery);
                    RetrieveResponse retrieveResponse = retrieveService.retrieveChunks(retrieveRequest);

                    StringBuilder ragContext = new StringBuilder();
                    ragContext.append("\n\nRelevant context:");
                    retrieveResponse.chunks().forEach(chunk -> {
                        ragContext.append("\n");
                        ragContext.append(chunk.getContent());
                    });
                    ragContext.append("\n\nBased on the above context, please answer the following question:\n");

                    String augmentedQuery = ragContext + userQuery;
                    ((ObjectNode) messages.get(userMessageIndex)).put(CONTENT, augmentedQuery);

                    String modifiedBody = objectMapper.writeValueAsString(rootNode);

                    return new AugmentResponse(modifiedBody);
                }
            }

            log.warn("No user message found in request, returning original");
            return new AugmentResponse(originalBodyJson);

        } catch (Exception e) {
            log.error("Failed to augment request: {}", e.getMessage(), e);
            return new AugmentResponse(originalBodyJson);
        }
    }
}