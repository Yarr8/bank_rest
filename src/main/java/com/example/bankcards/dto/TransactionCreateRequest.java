package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionCreateRequest {

    @NotBlank(message = "Source card number is required")
    private final String fromCardNumber;

    @NotBlank(message = "Destination card number is required")
    private final String toCardNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "100000000.00", message = "Amount must not exceed 100,000,000.00")
    private final BigDecimal amount;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private final String description;
}
