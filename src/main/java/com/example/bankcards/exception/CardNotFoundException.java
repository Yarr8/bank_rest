package com.example.bankcards.exception;

public class CardNotFoundException extends BusinessException {

    public CardNotFoundException(Long cardId) {
        super("Card not found with id: " + cardId);
    }

    public CardNotFoundException(String cardNumber) {
        super("Card not found with number: " + cardNumber);
    }
}
