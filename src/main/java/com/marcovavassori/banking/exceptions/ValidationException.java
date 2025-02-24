package com.marcovavassori.banking.exceptions;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Multiple validation errors occurred");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}