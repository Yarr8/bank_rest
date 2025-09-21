package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class UserBalanceResponse {
    private final BigDecimal totalBalance;
    private final List<CardBalance> cardBalances;

    @Data
    @AllArgsConstructor
    public static class CardBalance {
        private final Long cardId;
        private final String cardNumber;
        private final BigDecimal balance;
        private final String status;
    }
}
