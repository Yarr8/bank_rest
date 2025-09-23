package com.example.bankcards.controller;

import com.example.bankcards.dto.AdminCardCreateRequest;
import com.example.bankcards.dto.AdminCardUpdateRequest;
import com.example.bankcards.dto.AdminAuthRegisterRequest;
import com.example.bankcards.dto.CardBlockRequestResponse;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.UserCreateRequest;
import com.example.bankcards.dto.TransactionResponse;
import com.example.bankcards.dto.GenericErrorResponse;
import com.example.bankcards.dto.GenericSuccessResponse;
import com.example.bankcards.dto.UserUpdateRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtUser;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

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
    private final CardService cardService;
    private final TransactionService transactionService;
    private final CardBlockRequestService cardBlockRequestService;

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


    @PostMapping("/cards")
    public ResponseEntity<?> createCard(@Valid @RequestBody AdminCardCreateRequest request,
                                        Authentication authentication) {
        log.info("Creating card for user: {} by admin: {}", request.getUserId(), authentication.getName());

        try {
            Card card = Card.builder()
                    .cardNumber(request.getCardNumber())
                    .owner(request.getOwner())
                    .expiryDate(request.getExpiryDate())
                    .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                    .build();

            Card savedCard = cardService.createCard(card, request.getUserId());

            log.info("Card created successfully by admin: {} with id: {}",
                    authentication.getName(), savedCard.getId());

            return ResponseEntity.ok(new CardResponse(savedCard));

        } catch (Exception e) {
            log.error("Error creating card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to create card: " + e.getMessage()));
        }
    }

    @GetMapping("/cards")
    public ResponseEntity<?> getAllCards(Authentication authentication) {
        log.info("Admin {} getting all cards", authentication.getName());

        try {
            List<Card> cards = cardService.getAllCards();
            List<CardResponse> responses = cards.stream()
                    .map(CardResponse::new)
                    .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Error getting all cards: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get cards: " + e.getMessage()));
        }
    }

    @PutMapping("/cards/{id}/block")
    public ResponseEntity<?> blockCard(@PathVariable Long id, Authentication authentication) {
        log.info("Admin {} blocking card: {}", authentication.getName(), id);

        try {
            Card blockedCard = cardService.blockCard(id);

            log.info("Card blocked successfully by admin: {} with id: {}",
                    authentication.getName(), blockedCard.getId());

            return ResponseEntity.ok(new CardResponse(blockedCard));

        } catch (Exception e) {
            log.error("Error blocking card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to block card: " + e.getMessage()));
        }
    }

    @PutMapping("/cards/{id}/unblock")
    public ResponseEntity<?> unblockCard(@PathVariable Long id, Authentication authentication) {
        log.info("Admin {} unblocking card: {}", authentication.getName(), id);

        try {
            Card unblockedCard = cardService.unblockCard(id);

            log.info("Card unblocked successfully by admin: {} with id: {}",
                    authentication.getName(), unblockedCard.getId());

            return ResponseEntity.ok(new CardResponse(unblockedCard));

        } catch (Exception e) {
            log.error("Error unblocking card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to unblock card: " + e.getMessage()));
        }
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<?> deleteCard(@PathVariable Long id, Authentication authentication) {
        log.info("Admin {} deleting card: {}", authentication.getName(), id);

        try {
            cardService.deleteCard(id);

            log.info("Card deleted successfully by admin: {} with id: {}",
                    authentication.getName(), id);

            return ResponseEntity.ok(new GenericSuccessResponse("Card deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to delete card: " + e.getMessage()));
        }
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<?> updateCard(@PathVariable Long id,
                                        @Valid @RequestBody AdminCardUpdateRequest request,
                                        Authentication authentication) {
        log.info("Admin {} updating card: {}", authentication.getName(), id);

        try {
            Card card = cardService.getCardById(id);
            card.setOwner(request.getOwner());
            card.setExpiryDate(request.getExpiryDate());

            if (request.getStatus() != null) {
                card.setStatus(request.getStatus());
            }

            Card updatedCard = cardService.updateCard(card);

            log.info("Card updated successfully by admin: {} with id: {}",
                    authentication.getName(), updatedCard.getId());

            return ResponseEntity.ok(new CardResponse(updatedCard));

        } catch (Exception e) {
            log.error("Error updating card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to update card: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(Authentication authentication) {
        log.info("Admin {} getting all transactions", authentication.getName());

        try {
            List<Transaction> transactions = transactionService.getAllTransactions();
            List<TransactionResponse> responses = transactions.stream()
                    .map(TransactionResponse::new)
                    .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Error getting all transactions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/card-block-requests")
    public ResponseEntity<?> getAllCardBlockRequests(Authentication authentication) {
        log.info("Admin {} getting all card block requests", authentication.getName());

        try {
            List<CardBlockRequest> requests = cardBlockRequestService.getAllRequests();
            List<CardBlockRequestResponse> responses = requests.stream()
                    .map(CardBlockRequestResponse::new)
                    .toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("Error getting card block requests: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get card block requests: " + e.getMessage()));
        }
    }

    @GetMapping("/card-block-requests/status/{status}")
    public ResponseEntity<?> getCardBlockRequestsByStatus(@PathVariable String status,
                                                          Authentication authentication) {
        log.info("Admin {} getting card block requests by status: {}", authentication.getName(), status);

        try {
            CardBlockRequest.RequestStatus requestStatus = CardBlockRequest.RequestStatus.valueOf(status.toUpperCase());
            List<CardBlockRequest> requests = cardBlockRequestService.getRequestsByStatus(requestStatus);
            List<CardBlockRequestResponse> responses = requests.stream()
                    .map(CardBlockRequestResponse::new)
                    .toList();

            return ResponseEntity.ok(responses);

        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Invalid status: " + status));
        } catch (Exception e) {
            log.error("Error getting card block requests by status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get card block requests: " + e.getMessage()));
        }
    }

    @PutMapping("/card-block-requests/{id}/approve")
    public ResponseEntity<?> approveCardBlockRequest(@PathVariable Long id,
                                                     Authentication authentication) {
        log.info("Admin {} approving card block request: {}", authentication.getName(), id);

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User admin = userDetails.getUser();

            CardBlockRequest approvedRequest = cardBlockRequestService.approveRequest(id, admin.getId());

            log.info("Card block request approved successfully by admin: {} with id: {}",
                    authentication.getName(), id);

            return ResponseEntity.ok(new CardBlockRequestResponse(approvedRequest));

        } catch (Exception e) {
            log.error("Error approving card block request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to approve card block request: " + e.getMessage()));
        }
    }

    @PutMapping("/card-block-requests/{id}/reject")
    public ResponseEntity<?> rejectCardBlockRequest(@PathVariable Long id,
                                                    Authentication authentication) {
        log.info("Admin {} rejecting card block request: {}", authentication.getName(), id);

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User admin = userDetails.getUser();

            CardBlockRequest rejectedRequest = cardBlockRequestService.rejectRequest(id, admin.getId());

            log.info("Card block request rejected successfully by admin: {} with id: {}",
                    authentication.getName(), id);

            return ResponseEntity.ok(new CardBlockRequestResponse(rejectedRequest));

        } catch (Exception e) {
            log.error("Error rejecting card block request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to reject card block request: " + e.getMessage()));
        }
    }
}
