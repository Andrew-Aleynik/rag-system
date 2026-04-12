package com.andrewaleynik.ragsystem.chunkers;

import com.andrewaleynik.ragsystem.domains.ChunkDomain;
import com.andrewaleynik.ragsystem.domains.DocumentDomain;
import com.andrewaleynik.ragsystem.exceptions.ChunkingException;
import com.andrewaleynik.ragsystem.factories.ChunkFactory;
import com.andrewaleynik.universalparser.Analyzer;
import com.andrewaleynik.universalparser.Structure;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@RequiredArgsConstructor
public class AstChunker implements Chunker {
    private final Analyzer analyzer;
    private final int maxChunkSize;
    private final int minPayloadSize;
    private final float overlap;

    public List<ChunkDomain> chunkDocument(DocumentDomain document) throws ChunkingException, IOException {
        List<ChunkDomain> chunks;
        long fileSize = Files.size(document.getLocalPathAsPath());
        if (fileSize > 50_000_000) {
            throw new ChunkingException("File too large for AST parsing: " + fileSize);
        }
        String content = Files.readString(document.getLocalPathAsPath());
        Structure root = analyzer.analyze(content, 0).get(0);
        List<Structure> leafs = findLeafs(root);
        boolean stop = leafs.stream()
                .anyMatch(leaf -> maxChunkSize - calculateNonPayloadSize(leaf) < minPayloadSize);
        if (stop) {
            throw new ChunkingException("Document structure is too complex to chunk as AST");
        }

        String documentStructure = getDocumentStructure(content, root);
        List<ChunkDomain> structuralChunks = split(documentStructure);
        structuralChunks.forEach(chunk -> chunk.setStructural(true));
        chunks = new ArrayList<>(structuralChunks);

        leafs.forEach(leaf -> {
            String leafContent = getLeafContent(content, leaf);
            List<ChunkDomain> leafContentChunks = split(leafContent);
            leafContentChunks.forEach(chunk -> chunk.setStructural(false));
            chunks.addAll(leafContentChunks);
        });

        for (int i = 0; i < chunks.size(); i++) {
            ChunkDomain chunk = chunks.get(i);
            chunk.setDocumentId(document.getId());
            chunk.setIndex(i);
        }

        return chunks;
    }

    private List<Structure> findLeafs(Structure root) {
        List<Structure> leafs = new ArrayList<>();
        Deque<Structure> processingStack = new ArrayDeque<>();
        processingStack.push(root);
        while (!processingStack.isEmpty()) {
            Structure current = processingStack.pop();
            current.children.forEach(processingStack::push);
            if (current.children.isEmpty() && current.name.equals("foo")) {
                leafs.add(current);
            }
        }
        return leafs;
    }

    private String getDocumentStructure(String content, Structure root) {
        StringBuilder stringBuilder = new StringBuilder();
        Deque<Structure> forwardStack = new ArrayDeque<>();
        Deque<Structure> backwardStack = new ArrayDeque<>();
        forwardStack.addAll(root.children);
        while (!forwardStack.isEmpty()) {
            Structure current = forwardStack.pop();
            if (current.name.equals("declaration") || current.name.equals("var")) {
                stringBuilder.append("\n");
                stringBuilder.append(content, current.startIndex, current.endIndex);
            } else if (current.name.equals("highLevel")) {
                stringBuilder.append("\n");
                stringBuilder.append(content, current.startIndex, current.endPrefixIndex);
                forwardStack.addAll(current.children);
                backwardStack.push(current);
            } else {
                stringBuilder.append("\n");
                stringBuilder.append(content, current.startIndex, current.endPrefixIndex);
                stringBuilder.append("...");
                stringBuilder.append(content, current.endBodyIndex, current.endIndex);
            }
        }
        while (!backwardStack.isEmpty()) {
            Structure current = backwardStack.pop();
            stringBuilder.append("\n");
            stringBuilder.append(content, current.endBodyIndex, current.endIndex);
        }
        return stringBuilder.toString();
    }

    private int calculateNonPayloadSize(Structure leaf) {
        Structure current = leaf;
        int size = 0;
        while (current.parent != null) {
            current = current.parent;
            size += current.endPrefixIndex - current.startIndex;
            size += current.endIndex - current.endBodyIndex;
        }
        return size;
    }

    private String getLeafContent(String content, Structure leaf) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(content, leaf.startIndex, leaf.endIndex);
        Structure current = leaf.parent;
        while (current.parent != null) {
            stringBuilder.insert(0, content, current.startIndex, current.endPrefixIndex);
            stringBuilder.append(content, current.endBodyIndex, current.endIndex);
            current = current.parent;
        }
        return stringBuilder.toString();
    }

    private List<ChunkDomain> split(String content) {
        List<ChunkDomain> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String[] lines = content.split("\n", -1);

        int currentStartLine = 0;
        int currentEndLine = 0;
        int currentSize = 0;

        int overlapLines = Math.max(1, (int) (maxChunkSize * overlap) / 50);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineSize = line.length();

            if (lineSize > maxChunkSize) {
                if (currentSize > 0) {
                    chunks.add(createChunk(lines, currentStartLine, currentEndLine));
                }

                List<String> lineParts = splitLongLine(line, maxChunkSize);
                for (String part : lineParts) {
                    chunks.add(createChunkFromSingleLine(part));
                }

                currentStartLine = i + 1;
                currentEndLine = i;
                currentSize = 0;
                continue;
            }

            if (currentSize + lineSize > maxChunkSize && currentSize > 0) {
                chunks.add(createChunk(lines, currentStartLine, currentEndLine));

                currentStartLine = Math.max(0, currentEndLine - overlapLines);
                currentEndLine = i;
                currentSize = calculateSize(lines, currentStartLine, currentEndLine);
            }

            currentSize += lineSize;
            currentEndLine = i;
        }

        if (currentSize > 0) {
            chunks.add(createChunk(lines, currentStartLine, currentEndLine));
        }

        return chunks;
    }

    private ChunkDomain createChunk(String[] lines, int startLine, int endLine) {
        StringBuilder content = new StringBuilder();
        for (int i = startLine; i <= endLine; i++) {
            if (i > startLine) {
                content.append("\n");
            }
            content.append(lines[i]);
        }

        return createChunk(content.toString());
    }

    private ChunkDomain createChunkFromSingleLine(String line) {
        return createChunk(line);
    }

    private ChunkDomain createChunk(String content) {
        return new ChunkFactory()
                .withContent(content)
                .withSizeBytes(content.length())
                .withHash(computeHash(content))
                .createDomain();
    }

    private List<String> splitLongLine(String line, int maxSize) {
        List<String> parts = new ArrayList<>();
        int position = 0;

        while (position < line.length()) {
            int end = Math.min(position + maxSize * 3, line.length());
            parts.add(line.substring(position, end));
            position = end;
        }

        return parts;
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

    private int calculateSize(String[] lines, int startLine, int endLine) {
        int size = 0;
        for (int i = startLine; i <= endLine; i++) {
            size += lines[i].length();
        }
        return size;
    }
}
