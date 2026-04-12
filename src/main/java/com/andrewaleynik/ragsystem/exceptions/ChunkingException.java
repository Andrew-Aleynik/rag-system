package com.andrewaleynik.ragsystem.exceptions;

public class ChunkingException extends Exception {
    public ChunkingException(String message) {
        super(message);
    }

    public ChunkingException(String message, Throwable cause) {
        super(message, cause);
    }
}