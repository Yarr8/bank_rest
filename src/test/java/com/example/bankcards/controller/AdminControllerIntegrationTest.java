package com.example.bankcards.controller;

import com.example.bankcards.dto.AdminCardCreateRequest;
import com.example.bankcards.dto.AdminAuthRegisterRequest;
import com.example.bankcards.dto.UserCreateRequest;
import com.example.bankcards.dto.UserUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.util.CardMasker;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private CardBlockRequestService cardBlockRequestService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminJwtToken;
    private String userJwtToken;
    private User adminUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .password("password")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(2L)
                .username("testuser")
                .password("password")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserDetails adminUserDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(adminUser.getUsername())
                        .password(adminUser.getPassword())
                        .roles(adminUser.getRole().name())
                        .build();

        UserDetails regularUserDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(testUser.getUsername())
                        .password(testUser.getPassword())
                        .roles(testUser.getRole().name())
                        .build();

        adminJwtToken = jwtUtil.generateToken(adminUserDetails);
        userJwtToken = jwtUtil.generateToken(regularUserDetails);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsersForAdmin() throws Exception {
        List<User> users = Arrays.asList(adminUser, testUser);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("testuser"))
                .andExpect(jsonPath("$[1].role").value("USER"));
    }

    @Test
    void getAllUsers_ShouldReturnForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_ShouldReturnUserForAdmin() throws Exception {
        when(userService.findById(2L)).thenReturn(testUser);

        mockMvc.perform(get("/api/admin/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getUser_ShouldReturnNotFoundWhenUserNotExists() throws Exception {
        when(userService.findById(999L)).thenThrow(new RuntimeException("User not found with id: 999"));

        mockMvc.perform(get("/api/admin/users/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to get user: User not found with id: 999"));
    }

    @Test
    void createUser_ShouldCreateUserSuccessfullyForAdmin() throws Exception {
        User newUser = User.builder()
                .id(3L)
                .username("newuser")
                .password("password")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.createUser(any(User.class))).thenReturn(newUser);

        String requestBody = objectMapper.writeValueAsString(new UserCreateRequest(
                "newuser", "password", User.Role.USER
        ));

        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void createUser_ShouldReturnBadRequestWhenUsernameExists() throws Exception {
        when(userService.existsByUsername("testuser")).thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(new UserCreateRequest(
                "testuser", "password", User.Role.USER
        ));

        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void createUser_ShouldReturnForbiddenForRegularUser() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new UserCreateRequest(
                "newuser", "password", User.Role.USER
        ));

        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_ShouldCreateUserSuccessfullyForAdmin() throws Exception {
        User newUser = User.builder()
                .id(3L)
                .username("newuser")
                .password("password")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.existsByUsername("newuser")).thenReturn(false);
        when(userService.createUser(any(User.class))).thenReturn(newUser);

        String requestBody = objectMapper.writeValueAsString(new AdminAuthRegisterRequest(
                "newuser",
                "password123"
        ));

        mockMvc.perform(post("/api/admin/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_ShouldReturnBadRequestWhenUsernameExists() throws Exception {
        when(userService.existsByUsername("testuser")).thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(new AdminAuthRegisterRequest(
                "testuser",
                "password123"
        ));

        mockMvc.perform(post("/api/admin/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_ShouldReturnForbiddenForRegularUser() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new AdminAuthRegisterRequest(
                "newuser",
                "password123"
        ));

        mockMvc.perform(post("/api/admin/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_ShouldUpdateUserSuccessfullyForAdmin() throws Exception {
        User updatedUser = User.builder()
                .id(2L)
                .username("updateduser")
                .password("newpassword")
                .role(User.Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.findById(2L)).thenReturn(testUser);
        when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

        String requestBody = objectMapper.writeValueAsString(new UserUpdateRequest(
                "updateduser", "newpassword", User.Role.ADMIN
        ));

        mockMvc.perform(put("/api/admin/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void updateUser_ShouldReturnNotFoundWhenUserNotExists() throws Exception {
        when(userService.findById(999L)).thenThrow(new RuntimeException("User not found with id: 999"));

        String requestBody = objectMapper.writeValueAsString(new UserUpdateRequest(
                "updateduser", "newpassword", User.Role.ADMIN
        ));

        mockMvc.perform(put("/api/admin/users/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to update user: User not found with id: 999"));
    }

    @Test
    void deleteUser_ShouldDeleteUserSuccessfullyForAdmin() throws Exception {
        when(userService.findById(2L)).thenReturn(testUser);

        mockMvc.perform(delete("/api/admin/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    void deleteUser_ShouldReturnNotFoundWhenUserNotExists() throws Exception {
        doThrow(new RuntimeException("User not found with id: 999")).when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/admin/users/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to delete user: User not found with id: 999"));
    }

    @Test
    void deleteUser_ShouldReturnForbiddenForRegularUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersByRole_ShouldReturnUsersByRole() throws Exception {
        List<User> users = Arrays.asList(testUser);
        when(userService.getUsersByRole(User.Role.USER)).thenReturn(users);

        mockMvc.perform(get("/api/admin/users/role/USER")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void getUsersByRole_ShouldReturnBadRequestForInvalidRole() throws Exception {
        mockMvc.perform(get("/api/admin/users/role/INVALID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid role: INVALID"));
    }

    @Test
    void createCard_ShouldCreateCardSuccessfullyForAdmin() throws Exception {
        Card newCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("Test Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.createCard(any(Card.class), anyLong())).thenReturn(newCard);

        String requestBody = objectMapper.writeValueAsString(new AdminCardCreateRequest(
                2L, "1234567890123456", "Test Owner", LocalDate.now().plusYears(1), BigDecimal.ZERO
        ));

        String maskedCardNumber = CardMasker.maskCardNumber(newCard.getCardNumber());
        mockMvc.perform(post("/api/admin/cards")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardNumber").value(maskedCardNumber))
                .andExpect(jsonPath("$.owner").value("Test Owner"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void blockCard_ShouldBlockCardSuccessfullyForAdmin() throws Exception {
        Card blockedCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("Test Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.CardStatus.BLOCKED)
                .balance(BigDecimal.ZERO)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.blockCard(1L)).thenReturn(blockedCard);

        mockMvc.perform(put("/api/admin/cards/1/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void unblockCard_ShouldUnblockCardSuccessfullyForAdmin() throws Exception {
        Card unblockedCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("Test Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.unblockCard(1L)).thenReturn(unblockedCard);

        mockMvc.perform(put("/api/admin/cards/1/unblock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteCard_ShouldDeleteCardSuccessfullyForAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/cards/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card deleted successfully"));
    }

    @Test
    void getAllCardBlockRequests_ShouldReturnAllRequestsForAdmin() throws Exception {
        CardBlockRequest request = CardBlockRequest.builder()
                .id(1L)
                .card(Card.builder()
                        .id(1L)
                        .cardNumber("1234567890123456")
                        .owner("Test Owner")
                        .build())
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<CardBlockRequest> requests = Arrays.asList(request);
        when(cardBlockRequestService.getAllRequests()).thenReturn(requests);
        String maskedCardNumber = CardMasker.maskCardNumber(request.getCard().getCardNumber());

        mockMvc.perform(get("/api/admin/card-block-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].cardId").value(1))
                .andExpect(jsonPath("$[0].cardNumber").value(maskedCardNumber))
                .andExpect(jsonPath("$[0].requesterId").value(2))
                .andExpect(jsonPath("$[0].reason").value("Lost card"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getCardBlockRequestsByStatus_ShouldReturnRequestsByStatus() throws Exception {
        CardBlockRequest request = CardBlockRequest.builder()
                .id(1L)
                .card(Card.builder()
                        .id(1L)
                        .cardNumber("1234567890123456")
                        .owner("Test Owner")
                        .build())
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<CardBlockRequest> requests = Arrays.asList(request);
        when(cardBlockRequestService.getRequestsByStatus(CardBlockRequest.RequestStatus.PENDING)).thenReturn(requests);

        mockMvc.perform(get("/api/admin/card-block-requests/status/PENDING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approveCardBlockRequest_ShouldApproveRequestSuccessfully() throws Exception {
        CardBlockRequest approvedRequest = CardBlockRequest.builder()
                .id(1L)
                .card(Card.builder()
                        .id(1L)
                        .cardNumber("1234567890123456")
                        .owner("Test Owner")
                        .status(Card.CardStatus.BLOCKED)
                        .build())
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.APPROVED)
                .processedBy(adminUser)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardBlockRequestService.approveRequest(1L, 1L)).thenReturn(approvedRequest);

        mockMvc.perform(put("/api/admin/card-block-requests/1/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.processedById").value(1));
    }

    @Test
    void rejectCardBlockRequest_ShouldRejectRequestSuccessfully() throws Exception {
        CardBlockRequest rejectedRequest = CardBlockRequest.builder()
                .id(1L)
                .card(Card.builder()
                        .id(1L)
                        .cardNumber("1234567890123456")
                        .owner("Test Owner")
                        .status(Card.CardStatus.ACTIVE)
                        .build())
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.REJECTED)
                .processedBy(adminUser)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardBlockRequestService.rejectRequest(1L, 1L)).thenReturn(rejectedRequest);

        mockMvc.perform(put("/api/admin/card-block-requests/1/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.processedById").value(1));
    }

    @Test
    void updateCard_Success() throws Exception {
        LocalDate oldExpiryDate = LocalDate.of(2024, 12, 31);
        LocalDate newExpiryDate = LocalDate.of(2025, 12, 31);

        Card existingCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("Old Owner")
                .expiryDate(oldExpiryDate)
                .balance(BigDecimal.valueOf(1000))
                .status(Card.CardStatus.ACTIVE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Card updatedCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("New Owner")
                .expiryDate(newExpiryDate)
                .balance(BigDecimal.valueOf(1000))
                .status(Card.CardStatus.BLOCKED)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.getCardById(1L)).thenReturn(existingCard);
        when(cardService.updateCard(any(Card.class))).thenReturn(updatedCard);

        String updateRequest = """
                {
                    "owner": "New Owner",
                    "expiryDate": "2025-12-31",
                    "status": "BLOCKED"
                }
                """;

        mockMvc.perform(put("/api/admin/cards/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner").value("New Owner"))
                .andExpect(jsonPath("$.expiryDate").value("2025-12-31"))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void updateCard_CardNotFound() throws Exception {
        when(cardService.getCardById(999L)).thenThrow(new RuntimeException("Card not found"));

        String updateRequest = """
                {
                    "owner": "New Owner",
                    "expiryDate": "2025-12-31"
                }
                """;

        mockMvc.perform(put("/api/admin/cards/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to update card: Card not found"));
    }

    @Test
    void updateCard_InvalidData() throws Exception {
        String invalidRequest = """
                {
                    "owner": "",
                    "expiryDate": "invalid"
                }
                """;

        mockMvc.perform(put("/api/admin/cards/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
