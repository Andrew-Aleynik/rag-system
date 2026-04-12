package com.andrewaleynik.ragsystem.analyzers;

import com.andrewaleynik.universalparser.Analyzer;
import com.andrewaleynik.universalparser.AnalyzerBuilder;

public class DefaultAnalyzerConfig {
    private DefaultAnalyzerConfig() {
    }

    public static Analyzer createDefaultFileAnalyzer() {
        return new AnalyzerBuilder()
                .addLinear("all", "^", "$", all -> {
                })
                .build()
                .get(0);
    }
}
