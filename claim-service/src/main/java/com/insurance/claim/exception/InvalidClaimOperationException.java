package com.insurance.claim.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidClaimOperationException extends RuntimeException {
    public InvalidClaimOperationException(String message) { super(message); }
}
