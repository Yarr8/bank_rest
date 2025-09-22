package com.example.bankcards.exception;

public class DuplicateCardNumberException extends BusinessException {

    public DuplicateCardNumberException(String cardNumber) {
        super("Card number already exists: " + cardNumber);
    }
}
