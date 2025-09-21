package com.example.bankcards.exception;

public class InvalidTransactionException extends BusinessException {

    public InvalidTransactionException(String message) {
        super("Invalid transaction: " + message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super("Invalid transaction: " + message, cause);
    }
}
