package com.fashion.auth.exception;

public class GhnIntegrationException extends RuntimeException {
    public GhnIntegrationException(String message) {
        super(message);
    }

    public GhnIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
