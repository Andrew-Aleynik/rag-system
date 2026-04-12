package com.andrewaleynik.ragsystem.chunkers;

import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;
import com.andrewaleynik.ragsystem.factories.ChunkFactory;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
public class DefaultChunker implements Chunker {
    private final int maxChunkSize;
    private final float overlap;

    @Override
    public List<ChunkDomain> chunkDocument(DocumentDomain document) throws ChunkingException, IOException {
        String content = Files.readString(document.getLocalPathAsPath());

        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChunkDomain> chunks = new ArrayList<>();

        int step = (int) (maxChunkSize * (1 - overlap));

        List<String> chunkContents = splitByCharacters(content, maxChunkSize, step);

        for (int i = 0; i < chunkContents.size(); i++) {
            String chunkContent = chunkContents.get(i);
            ChunkDomain chunk = new ChunkFactory()
                    .withContent(chunkContent)
                    .withSizeBytes(chunkContent.length())
                    .withHash(computeHash(chunkContent))
                    .createDomain();

            chunk.setDocumentId(document.getId());
            chunk.setIndex(i);

            chunks.add(chunk);
        }

        return chunks;
    }

    private List<String> splitByCharacters(String text, int chunkSize, int step) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int position = 0;
        while (position < text.length()) {
            int end = Math.min(position + chunkSize, text.length());

            String chunk = extractChunkWithBoundary(text, position, end);
            chunks.add(chunk);

            position += step;
        }

        return chunks;
    }

    private String extractChunkWithBoundary(String text, int start, int end) {
        if (end >= text.length()) {
            return text.substring(start);
        }

        int boundary = findBoundary(text, end);

        if (boundary > start && boundary > end - maxChunkSize / 4) {
            return text.substring(start, boundary);
        } else {
            return text.substring(start, end);
        }
    }

    private int findBoundary(String text, int position) {
        int maxLookahead = Math.min(50, text.length() - position);

        for (int i = position; i < position + maxLookahead && i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t' ||
                    c == '.' || c == '!' || c == '?' || c == ';' || c == ':') {
                return i + 1;
            }
        }

        return position;
    }

    // FNV-1a
    private String computeHash(String content) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : content.getBytes()) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return Long.toHexString(hash);
    }
}