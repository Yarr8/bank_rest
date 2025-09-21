package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthLoginRequest {

    @NotBlank(message = "Username is required")
    private final String username;

    @NotBlank(message = "Password is required")
    private final String password;
}
