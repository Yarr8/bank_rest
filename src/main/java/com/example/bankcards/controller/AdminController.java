package com.example.bankcards.controller;

import com.example.bankcards.dto.AdminAuthRegisterRequest;
import com.example.bankcards.dto.GenericErrorResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AdminAuthRegisterRequest request,
                                      Authentication authentication) {
        log.info("Registration attempt for user: {} by admin: {}", request.getUsername(), authentication.getName());

        try {
            if (userService.existsByUsername(request.getUsername())) {
                log.error("Username already exists: {}", request.getUsername());
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Username already exists"));
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .role(User.Role.USER)
                    .build();

            User savedUser = userService.createUser(user);

            log.info("Registration successful for user: {} by admin: {}", request.getUsername(), authentication.getName());

            return ResponseEntity.ok(new UserResponse(savedUser));

        } catch (Exception e) {
            log.error("Registration failed for user: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
}
