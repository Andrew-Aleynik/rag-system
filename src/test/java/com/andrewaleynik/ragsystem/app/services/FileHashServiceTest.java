package com.andrewaleynik.ragsystem.app.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class FileHashServiceTest {
    private final FileHashService fileHashService = new FileHashService();

    @ParameterizedTest
    @MethodSource("files")
    void testSuccessCalculateHash(Path file) throws IOException {
        Assertions.assertDoesNotThrow(() -> fileHashService.calculateHash(file));
    }

    @ParameterizedTest
    @MethodSource("sameContentFiles")
    void testSameContentFilesHashesEquals(Path file1, Path file2) throws IOException {
        String hash1 = fileHashService.calculateHash(file1);
        String hash2 = fileHashService.calculateHash(file2);

        Assertions.assertEquals(hash1, hash2);
    }

    @ParameterizedTest
    @MethodSource("differentContentFiles")
    void testDifferentContentFilesHashesNotEquals(Path file1, Path file2) throws IOException {
        String hash1 = fileHashService.calculateHash(file1);
        String hash2 = fileHashService.calculateHash(file2);

        Assertions.assertNotEquals(hash1, hash2);
    }

    static Stream<Arguments> files() {
        return Stream.of(
                Arguments.of(resource("files/class.java")),
                Arguments.of(resource("files/script.py")),
                Arguments.of(resource("files/script.sh")),
                Arguments.of(resource("files/empty"))
        );
    }

    static Stream<Arguments> sameContentFiles() {
        return Stream.of(
                Arguments.of(resource("files/class.java"), resource("files/class2.java")),
                Arguments.of(resource("files/empty"), resource("files/empty2"))
        );
    }

    static Stream<Arguments> differentContentFiles() {
        return Stream.of(
                Arguments.of(resource("files/class.java"), resource("files/empty")),
                Arguments.of(resource("files/script.py"), resource("files/script.sh"))
        );
    }

    private static Path resource(String path) {
        try {
            return Paths.get(FileHashServiceTest.class.getClassLoader()
                    .getResource(path)
                    .toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI for resource: " + path, e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Resource not found: " + path, e);
        }
    }
}