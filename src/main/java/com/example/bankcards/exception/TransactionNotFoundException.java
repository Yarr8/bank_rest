package com.example.bankcards.exception;

public class TransactionNotFoundException extends BusinessException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found with id: " + id);
    }

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
