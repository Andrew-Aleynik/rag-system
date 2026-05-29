package com.andrewaleynik.ragsystem.chunkers;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractChunker implements Chunker {

    // FNV-1a
    protected String computeHash(String content) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : content.getBytes()) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return Long.toHexString(hash);
    }

    protected String readFileWithAutoEncoding(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);

        // Определяем кодировку
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        detector.reset();

        Charset charset = encoding != null
                ? Charset.forName(encoding)
                : StandardCharsets.UTF_8;

        return new String(bytes, charset);
    }
}
