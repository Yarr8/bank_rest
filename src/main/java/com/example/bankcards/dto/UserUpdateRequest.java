package com.example.bankcards.dto;

import com.example.bankcards.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private final String username;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private final String password;

    @NotNull(message = "Role is required")
    private final User.Role role;
}
