package com.example.bankcards.dto;

import com.example.bankcards.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private final Long id;
    private final String username;
    private final String role;
    private final String createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getRole().name();
        this.createdAt = user.getCreatedAt().toString();
    }
}
