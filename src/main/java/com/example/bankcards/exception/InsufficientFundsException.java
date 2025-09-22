package com.example.bankcards.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(BigDecimal required, BigDecimal available) {
        super(String.format("Insufficient funds. Required: %s, Available: %s", required, available));
    }

    public InsufficientFundsException(String message) {
        super(message);
    }
}
