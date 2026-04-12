package com.andrewaleynik.ragsystem;

import com.andrewaleynik.ragsystem.analyzers.DefaultAnalyzerConfig;
import com.andrewaleynik.ragsystem.analyzers.JavaAnalyzerConfig;
import com.andrewaleynik.universalparser.Analyzer;
import com.andrewaleynik.universalparser.Structure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;
import java.util.stream.Stream;

@Disabled("Manual test")
class AnalyzerConfigTest {
    @ParameterizedTest
    @MethodSource("analyzersSamples")
    void testAnalyzerConfig(Analyzer analyzer, URL sampleResource) throws IOException {
        String content = Files.readString(Path.of(sampleResource.getPath()));
        Structure structure = analyzer.analyze(content, 0).get(0);
        Stack<Structure> structures = new Stack<>();
        structures.add(structure);
        while (!structures.isEmpty()) {
            Structure currentStructure = structures.pop();
            System.out.println("=".repeat(100));
            System.out.println(currentStructure);
            System.out.println(content.substring(currentStructure.startIndex, currentStructure.endIndex));
            System.out.println("=".repeat(100) + "\n\n");
            currentStructure.children.forEach(structures::push);
        }
        Assertions.assertTrue(true);
    }

    private static Stream<Arguments> analyzersSamples() {
        return Stream.of(
                Arguments.of(JavaAnalyzerConfig.createJavaFileAnalyzer(),
                        AnalyzerConfigTest.class.getClassLoader().getResource("samples/sample1.java")),
                Arguments.of(JavaAnalyzerConfig.createJavaFileAnalyzer(),
                        AnalyzerConfigTest.class.getClassLoader().getResource("samples/sample2.java")),
                Arguments.of(JavaAnalyzerConfig.createJavaFileAnalyzer(),
                        AnalyzerConfigTest.class.getClassLoader().getResource("samples/sample3.java")),
                Arguments.of(JavaAnalyzerConfig.createJavaFileAnalyzer(),
                        AnalyzerConfigTest.class.getClassLoader().getResource("samples/sample4.java")),
                Arguments.of(DefaultAnalyzerConfig.createDefaultFileAnalyzer(),
                        AnalyzerConfigTest.class.getClassLoader().getResource("samples/sample1.java"))
        );
    }
}
