package com.marcovavassori.banking.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        // 400 Bad Request - Client sent invalid data
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(ValidationException e) {
        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                e.getErrors());
        // 400 Bad Request - Client sent invalid data
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        // 409 Conflict - Client sent invalid data
        return createErrorResponse(HttpStatus.CONFLICT, "Email Already Exists", e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        // 404 Not Found - Resource not found
        return createErrorResponse(HttpStatus.NOT_FOUND, "User Not Found", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        // 400 Bad Request - Client sent invalid arguments
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Arguments", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        // 401 Unauthorized - Authentication failed
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed", e.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException e) {
        // 404 Not Found - Resource not found
        return createErrorResponse(HttpStatus.NOT_FOUND, "Account Not Found", e.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException e) {
        // 400 Bad Request - Client sent invalid data
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Insufficient Balance", e.getMessage());
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(InvalidTransactionException e) {
        // 400 Bad Request - Client sent invalid data
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Transaction", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        // 500 Internal Server Error - Unexpected errors
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred");
    }

    // Helper method to create an error response
    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status, String error, String message) {
        ErrorResponse response = new ErrorResponse(status.value(), error, message);
        return new ResponseEntity<>(response, status);
    }

    // record for the error response structure
    private record ErrorResponse(
            int status,
            String error,
            String message) {
    }

    // record for validation error responses
    private record ValidationErrorResponse(
            int status,
            String error,
            List<String> messages) {
    }
}
