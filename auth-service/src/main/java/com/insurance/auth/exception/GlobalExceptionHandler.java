package com.insurance.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * Catches ALL exceptions thrown anywhere in the service and returns
 * a consistent, structured JSON error response.
 *
 * Without this, Spring would return its default ugly HTML/JSON error.
 * With this, every error looks like:
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "...",
 *   "errors": { "fieldName": "validation message" },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Validation Errors (400) ─────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                (existing, replacement) -> existing  // keep first error per field
            ));

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed. Check 'errors' field for details.")
            .errors(fieldErrors)
            .path(extractPath(request))
            .timestamp(LocalDateTime.now())
            .build();

        log.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    // ─── Custom Business Exceptions ──────────────────────────────────────────

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
            AccountDisabledException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ─── Spring Security Exceptions ──────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
            "Invalid credentials", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN,
            "You don't have permission to access this resource", request);
    }

    // ─── Catch-All ───────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again or contact support.", request);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message, WebRequest request) {

        ErrorResponse error = ErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(extractPath(request))
            .timestamp(LocalDateTime.now())
            .build();

        log.warn("Error response [{}]: {}", status.value(), message);
        return ResponseEntity.status(status).body(error);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    // ─── Error Response Body ─────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private Map<String, String> errors;
        private String path;
        private LocalDateTime timestamp;
    }
}
