package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenericErrorResponse {
    private final String error;
}
