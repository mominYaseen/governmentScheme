package com.schemenavigator.exception;

public class SchemeNavigatorException extends RuntimeException {

    public SchemeNavigatorException(String message) {
        super(message);
    }

    public SchemeNavigatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
