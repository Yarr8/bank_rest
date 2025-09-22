package com.example.bankcards.exception;

public class CardBlockRequestNotFoundException extends BusinessException {

    public CardBlockRequestNotFoundException(Long requestId) {
        super("Card block request not found with id: " + requestId);
    }
}
