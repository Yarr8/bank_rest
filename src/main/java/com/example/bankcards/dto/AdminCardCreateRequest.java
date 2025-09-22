package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AdminCardCreateRequest {

    @NotNull(message = "User ID is required")
    private final Long userId;

    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    private final String cardNumber;

    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    private final String owner;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private final LocalDate expiryDate;

    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @DecimalMax(value = "100000000.00", message = "Balance must not exceed 100,000,000.00")
    private final BigDecimal balance;
}
