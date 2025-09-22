package com.example.bankcards.controller;

import com.example.bankcards.dto.AdminAuthRegisterRequest;
import com.example.bankcards.dto.UserCreateRequest;
import com.example.bankcards.dto.GenericErrorResponse;
import com.example.bankcards.dto.GenericSuccessResponse;
import com.example.bankcards.dto.UserUpdateRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

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

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        log.info("Getting all users for admin: {}", authentication.getName());

        try {
            List<User> users = userService.getAllUsers();

            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::new)
                    .toList();

            return ResponseEntity.ok(userResponses);

        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id, Authentication authentication) {
        log.info("Getting user: {} for admin: {}", id, authentication.getName());

        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(new UserResponse(user));

        } catch (Exception e) {
            log.error("Error getting user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get user: " + e.getMessage()));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest request,
                                        Authentication authentication) {
        log.info("Creating user: {} by admin: {}", request.getUsername(), authentication.getName());

        try {
            if (userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Username already exists"));
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .role(request.getRole())
                    .build();

            User savedUser = userService.createUser(user);

            log.info("User created successfully by admin: {} with id: {}",
                    authentication.getName(), savedUser.getId());

            return ResponseEntity.ok(new UserResponse(savedUser));

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to create user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UserUpdateRequest request,
                                        Authentication authentication) {
        log.info("Updating user: {} by admin: {}", id, authentication.getName());

        try {
            User user = userService.findById(id);
            user.setUsername(request.getUsername());
            user.setRole(request.getRole());

            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(request.getPassword());
            }

            User updatedUser = userService.updateUser(user);

            log.info("User updated successfully by admin: {}", authentication.getName());

            return ResponseEntity.ok(new UserResponse(updatedUser));

        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to update user: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        log.info("Deleting user: {} by admin: {}", id, authentication.getName());

        try {
            userService.deleteUser(id);

            log.info("User deleted successfully by admin: {}", authentication.getName());

            return ResponseEntity.ok(new GenericSuccessResponse("User deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to delete user: " + e.getMessage()));
        }
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role, Authentication authentication) {
        log.info("Getting users by role: {} for admin: {}", role, authentication.getName());

        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            List<User> users = userService.getUsersByRole(userRole);

            List<UserResponse> userResponses = users.stream()
                    .map(UserResponse::new)
                    .toList();

            return ResponseEntity.ok(userResponses);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Invalid role: " + role));
        } catch (Exception e) {
            log.error("Error getting users by role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get users: " + e.getMessage()));
        }
    }
}
