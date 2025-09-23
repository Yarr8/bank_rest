package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminCardUpdateRequest {

    @NotBlank(message = "Cardholder name is required")
    @Size(min = 2, max = 100, message = "Cardholder name must be between 2 and 100 characters")
    private String owner;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private final LocalDate expiryDate;

    private Card.CardStatus status;
}
