package com.andrewaleynik.ragsystem.exceptions;

public class RepositoryConnectionException extends RuntimeException {
    public RepositoryConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
