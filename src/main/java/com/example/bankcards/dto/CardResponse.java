package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CardResponse {

    private final Long id;
    private final String cardNumber;
    private final String owner;
    private final LocalDate expiryDate;
    private final String status;
    private final BigDecimal balance;
    private final String createdAt;

    public CardResponse(Card card) {
        this.id = card.getId();
        this.cardNumber = com.example.bankcards.util.CardMasker.maskCardNumber(card.getCardNumber());
        this.owner = card.getOwner();
        this.expiryDate = card.getExpiryDate();
        this.status = card.getStatus().name();
        this.balance = card.getBalance();
        this.createdAt = card.getCreatedAt().toString();
    }
}
