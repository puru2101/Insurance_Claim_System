package com.insurance.policy.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorBody> notFound(ResourceNotFoundException ex, WebRequest req) {
        return ResponseEntity.status(404).body(ErrorBody.of(404, "Not Found", ex.getMessage(), req, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid", (a, b) -> a));
        return ResponseEntity.badRequest().body(ErrorBody.of(400, "Validation Failed", "Check errors field", req, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> general(Exception ex, WebRequest req) {
        log.error("Unhandled: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500).body(ErrorBody.of(500, "Internal Server Error", "Unexpected error", req, null));
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {
        private int status; private String error; private String message;
        private Map<String, String> errors; private String path; private LocalDateTime timestamp;

        public static ErrorBody of(int s, String e, String m, WebRequest r, Map<String, String> errs) {
            return ErrorBody.builder().status(s).error(e).message(m).errors(errs)
                .path(r.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).build();
        }
    }
}
