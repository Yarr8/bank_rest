package com.example.bankcards.exception;

import com.example.bankcards.dto.GenericErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericErrorResponse> handleBusinessException(BusinessException ex) {
        log.error("Business exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new GenericErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation exception: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericErrorResponse("Internal server error"));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<GenericErrorResponse> handleInsufficientFundsException(InsufficientFundsException ex) {
        log.error("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new GenericErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CardBlockedException.class)
    public ResponseEntity<GenericErrorResponse> handleCardBlockedException(CardBlockedException ex) {
        log.error("Card blocked: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new GenericErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<GenericErrorResponse> handleInvalidTransactionException(InvalidTransactionException ex) {
        log.error("Invalid transaction: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new GenericErrorResponse(ex.getMessage()));
    }

@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    String message = "Invalid request body";

    if (ex.getCause() instanceof InvalidFormatException) {
        InvalidFormatException cause = (InvalidFormatException) ex.getCause();
        if (cause.getTargetType() == LocalDate.class) {
            message = "Invalid date format. Expected format: YYYY-MM-DD";
        }
    }

    return ResponseEntity.badRequest()
        .body(new GenericErrorResponse(message));
}
}
