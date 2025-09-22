package com.example.bankcards.exception;

public class CardBlockedException extends BusinessException {

    public CardBlockedException(String message) {
        super(message);
    }

    public CardBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
