package com.andrewaleynik.ragsystem.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    @Test
    void testParsing() throws IOException {


        assertTrue(true);
    }

    private Path resource(String path) {
        try {
            return Path.of(ParserTest.class.getClassLoader()
                    .getResource(path)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
