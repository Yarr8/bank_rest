package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokenRequest {

    @NotBlank(message = "Token is required")
    private final String token;
}
