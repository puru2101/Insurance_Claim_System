package com.insurance.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
            .stream().collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid",
                (a, b) -> a));
        return ResponseEntity.badRequest().body(ErrorResponse.of(400,
            "Validation Failed", "Check errors field", req, fieldErrors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        return ResponseEntity.status(404).body(ErrorResponse.of(404, "Not Found", ex.getMessage(), req, null));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, WebRequest req) {
        return ResponseEntity.status(409).body(ErrorResponse.of(409, "Conflict", ex.getMessage(), req, null));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorage(StorageException ex, WebRequest req) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Storage Error", ex.getMessage(), req, null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, WebRequest req) {
        return ResponseEntity.status(413).body(ErrorResponse.of(413, "Payload Too Large",
            "File size exceeds the maximum allowed limit of 10MB", req, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500).body(ErrorResponse.of(500,
            "Internal Server Error", "An unexpected error occurred", req, null));
    }

    // ── Error body ────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private Map<String, String> errors;
        private String path;
        private LocalDateTime timestamp;

        public static ErrorResponse of(int status, String error, String message,
                                       WebRequest req, Map<String, String> errors) {
            return ErrorResponse.builder()
                .status(status).error(error).message(message).errors(errors)
                .path(req.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now()).build();
        }
    }
}
