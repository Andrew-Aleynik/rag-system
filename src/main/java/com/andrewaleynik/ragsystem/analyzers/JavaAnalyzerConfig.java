package com.andrewaleynik.ragsystem.analyzers;

import com.andrewaleynik.universalparser.Analyzer;
import com.andrewaleynik.universalparser.AnalyzerBuilder;

public class JavaAnalyzerConfig {

    private JavaAnalyzerConfig() {
    }

    public static Analyzer createJavaFileAnalyzer() {
        return new AnalyzerBuilder()
                .addLinear("all", "^", "$", all -> all
                        .addLinear("declaration", // Package
                                "package\\s+",
                                ";",
                                ignored -> {
                                })

                        .addRepeatable("declaration", // Imports
                                "import\\s+",
                                ";",
                                ignored -> {
                                })

                        .addRecursive("highLevel",
                                "(?s)(?://[^\\n]*\\n\\s*)?(?:/\\*\\*?[^*]*\\*+(?:[^/*][^*]*\\*+)*/\\s*)?(?:@\\w+(?:\\s*\\(\\s*(?:\"[^\"]*\"|'[^']*'|[\\w.]+|[^)]*)\\s*\\))?\\s*)*\\b(?:public\\s+|protected\\s+|private\\s+)?(?:abstract\\s+|final\\s+|strictfp\\s+)?(?:static\\s+)?(?:class|interface|enum|@interface|record)\\s+(\\w+(?:<[^>]*>)?)(?:\\s+extends\\s+(\\w+(?:<[^>]*>)?))?(?:\\s+implements\\s+([\\w,<.>\\s]+?))?\\s*\\{",
                                "\\}", "\\{", "\\}",
                                highLevelAnalyzerBuilder -> highLevelAnalyzerBuilder
                                        .addRepeatable("foo", //Method or constructor
                                                "(?s)(?://[^\\n]*\\n\\s*)?(?:/\\*\\*?[^*]*\\*+(?:[^/*][^*]*\\*+)*/\\s*)?" +
                                                        "(?:@[a-zA-Z_$][\\w$]*(?:\\s*\\([^)]*\\))?\\s*)*" +
                                                        "(?:public\\s+|protected\\s+|private\\s+)?" +
                                                        "(?:abstract\\s+|static\\s+|final\\s+|synchronized\\s+|native\\s+|strictfp\\s+)*" +
                                                        "(?!" +
                                                        "\\s*(?:if|while|for|do|switch|try|catch|finally|synchronized)\\s*\\(" +
                                                        ")" +
                                                        "(?:" +
                                                        "(?:<[^>]*>\\s+)?" +
                                                        "(?!\\s*(?:return|throw|break|continue|assert|new|this|super|null|true|false)\\s)" +
                                                        "(?:[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*(?:\\s*<[^>]*>)?(?:\\s*\\[\\s*\\])*(?:\\.\\.\\.)?)\\s+[a-zA-Z_$][\\w$]*\\s*\\(" +
                                                        "|" +
                                                        "(?!\\s*record\\s)" +
                                                        "(?!\\s*(?:return|throw|break|continue|assert|new|this|super|null|true|false|var|int|long|double|float|boolean|byte|short|char|void|[a-z])\\s)" +
                                                        "[A-Z][a-zA-Z0-9_$]*\\s*\\(" +
                                                        ")" +
                                                        "(?:[^()]|\\((?:[^()]|\\([^)]*\\))*\\))*" +
                                                        "\\)" +
                                                        "(?:\\s*throws\\s+[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*(?:\\s*<[^>]*>)?(?:\\s*,\\s*[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*(?:\\s*<[^>]*>)?)*)?" +
                                                        "\\s*\\{",
                                                "\\}", "\\{", "\\}",
                                                ignored -> {
                                                }
                                        )
                                        .addRepeatable("foo", //Abstract method
                                                "(?s)(?://[^\\n]*\\n\\s*)?(?:/\\*\\*?[^*]*\\*+(?:[^/*][^*]*\\*+)*/\\s*)?" +
                                                        "(?:@[a-zA-Z_$][\\w$]*(?:\\s*\\([^)]*\\))?\\s*)*" +
                                                        "(?:public\\s+|protected\\s+|private\\s+)?" +
                                                        "(?:abstract\\s+|static\\s+|default\\s+)*" +
                                                        "(?:<[^>]*>\\s+)?" +
                                                        "(?!\\s*(?:return|throw|break|continue|assert|new|this|super|null|true|false)\\s)" +
                                                        "(?:[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*(?:\\s*<[^>]*>)?(?:\\s*\\[\\s*\\])*(?:\\.\\.\\.)?)\\s+[a-zA-Z_$][\\w$]*\\s*\\(" +
                                                        "(?:[^()]|\\((?:[^()]|\\([^)]*\\))*\\))*" +
                                                        "\\)" +
                                                        "(?:\\s*throws\\s+[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*(?:\\s*<[^>]*>)?(?:\\s*,\\s*[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*(?:\\s*<[^>]*>)?)*)?" +
                                                        "\\s*;",
                                                "",
                                                ignored -> {
                                                }
                                        )
                                        .addRepeatable("var",
                                                "(?s)(?://[^\\n]*\\n\\s*)?(?:/\\*\\*?[^*]*\\*+(?:[^/*][^*]*\\*+)*/\\s*)?" +
                                                        "(?:@[a-zA-Z_$][\\w$]*(?:\\s*\\([^)]*\\))?\\s*)*" +
                                                        "(?!\\s*(?:if|while|for|do|switch|return|throw|break|continue|assert|synchronized|try|catch|finally|case|default)\\s*[(;{}@\"']|\\s*\\w+\\s*:)" +
                                                        "(?:public\\s+|protected\\s+|private\\s+)?" +
                                                        "(?:static\\s+)?" +
                                                        "(?:final\\s+)?" +
                                                        "(?:transient\\s+)?" +
                                                        "(?:volatile\\s+)?" +
                                                        "\\b(?!\\s*(?:if|while|for|do|switch|return|throw|break|continue|assert|synchronized|try|catch|finally|case|default|new|this|super|null|true|false|void|var|int|long|double|float|boolean|byte|short|char)\\s)" +
                                                        "(?:[a-zA-Z_$][\\w$]*(?:\\s*\\.\\s*[a-zA-Z_$][\\w$]*)*" +
                                                        "(?:\\s*<[^>]*>)?" +
                                                        "(?:\\s*\\[\\s*\\])*)\\s+" +
                                                        "[a-zA-Z_$][\\w$]*" +
                                                        "(?!\\s*\\()" +
                                                        "(?:\\s*=\\s*(?:" +
                                                        "[^{;]*" +
                                                        "|" +
                                                        "\\s*switch\\s*\\([^)]*\\)\\s*\\{" +
                                                        "(?:[^{}]|\\{(?:[^{}]|\\{[^{}]*\\})*\\})*" +
                                                        "\\}" +
                                                        "))?" +
                                                        "\\s*;",
                                                "",
                                                ignored -> {
                                                }
                                        )
                                        .addRepeatable("var",
                                                "static\\s*\\{",
                                                "\\}", "\\{", "\\}",
                                                initBuilder -> {
                                                }
                                        )
                        )
                )
                .build()
                .get(0);
    }
}