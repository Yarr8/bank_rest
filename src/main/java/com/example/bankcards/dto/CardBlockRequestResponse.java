package com.example.bankcards.dto;

import com.example.bankcards.entity.CardBlockRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

import com.example.bankcards.util.CardMasker;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CardBlockRequestResponse {
    private final Long id;
    private final Long cardId;
    private final String cardNumber;
    private final Long requesterId;
    private final String requesterUsername;
    private final String reason;
    private final String status;
    private final Long processedById;
    private final String processedByUsername;
    private final LocalDateTime processedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CardBlockRequestResponse(CardBlockRequest request) {
        this.id = request.getId();
        this.cardId = request.getCard().getId();
        this.cardNumber = CardMasker.maskCardNumber(request.getCard().getCardNumber());
        this.requesterId = request.getRequester().getId();
        this.requesterUsername = request.getRequester().getUsername();
        this.reason = request.getReason();
        this.status = request.getStatus().name();
        this.processedById = request.getProcessedBy() != null ? request.getProcessedBy().getId() : null;
        this.processedByUsername = request.getProcessedBy() != null ? request.getProcessedBy().getUsername() : null;
        this.processedAt = request.getProcessedAt();
        this.createdAt = request.getCreatedAt();
        this.updatedAt = request.getUpdatedAt();
    }
}
