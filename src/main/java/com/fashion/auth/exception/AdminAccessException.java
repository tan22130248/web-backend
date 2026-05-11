package com.fashion.auth.exception;

import org.springframework.http.HttpStatus;

public class AdminAccessException extends RuntimeException {
    private final HttpStatus status;

    public AdminAccessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
