package com.example.bankcards.dto;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.util.CardMasker;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private final Long id;
    private final String fromCardNumber;
    private final String toCardNumber;
    private final BigDecimal amount;
    private final String status;
    private final String description;
    private final String createdAt;
    private final String processedAt;

    public TransactionResponse(Transaction transaction) {
        this.id = transaction.getId();
        this.fromCardNumber = CardMasker.maskCardNumber(transaction.getFromCard().getCardNumber());
        this.toCardNumber = CardMasker.maskCardNumber(transaction.getToCard().getCardNumber());
        this.amount = transaction.getAmount();
        this.status = transaction.getStatus().name();
        this.description = transaction.getDescription();
        this.createdAt = transaction.getCreatedAt().toString();
        this.processedAt = transaction.getProcessedAt() != null ?
                transaction.getProcessedAt().toString() : null;
    }
}
