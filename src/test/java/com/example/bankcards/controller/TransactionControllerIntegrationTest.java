package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionCreateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.CardMasker;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CardRepository cardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String jwtToken;
    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(User.Role.USER)
                .build();

        fromCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("Test Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .user(testUser)
                .build();

        toCard = Card.builder()
                .id(2L)
                .cardNumber("9876543210987654")
                .owner("Test Owner 2")
                .expiryDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .user(testUser)
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(BigDecimal.valueOf(100))
                .description("Test transaction")
                .status(Transaction.TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        UserDetails testUserDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(testUser.getUsername())
                        .password(testUser.getPassword())
                        .roles(testUser.getRole().name())
                        .build();

        jwtToken = jwtUtil.generateToken(testUserDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void createTransaction_ShouldCreateTransactionSuccessfullyBetweenOwnCards() throws Exception {
        when(cardService.getCardByNumber("1234567890123456")).thenReturn(java.util.Optional.of(fromCard));
        when(cardService.getCardByNumber("9876543210987654")).thenReturn(java.util.Optional.of(toCard));
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(true);
        when(cardService.isCardOwnedByUser(2L, 1L)).thenReturn(true);
        when(transactionService.createTransaction(any(Transaction.class))).thenReturn(testTransaction);

        String requestBody = objectMapper.writeValueAsString(new TransactionCreateRequest(
                "1234567890123456", "9876543210987654", BigDecimal.valueOf(100), "Test transaction"
        ));

        String maskedFromCardNumber = CardMasker.maskCardNumber(fromCard.getCardNumber());
        String maskedToCardNumber = CardMasker.maskCardNumber(toCard.getCardNumber());

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fromCardNumber").value(maskedFromCardNumber))
                .andExpect(jsonPath("$.toCardNumber").value(maskedToCardNumber))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createTransaction_ShouldReturnBadRequestWhenSourceCardNotOwned() throws Exception {
        when(cardService.getCardByNumber("1234567890123456")).thenReturn(java.util.Optional.of(fromCard));
        when(cardService.getCardByNumber("9876543210987654")).thenReturn(java.util.Optional.of(toCard));
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(false);

        String requestBody = objectMapper.writeValueAsString(new TransactionCreateRequest(
                "1234567890123456", "9876543210987654", BigDecimal.valueOf(100), "Test transaction"
        ));

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Access denied to source card"));
    }

    @Test
    void createTransaction_ShouldReturnBadRequestWhenDestinationCardNotOwned() throws Exception {
        when(cardService.getCardByNumber("1234567890123456")).thenReturn(java.util.Optional.of(fromCard));
        when(cardService.getCardByNumber("9876543210987654")).thenReturn(java.util.Optional.of(toCard));
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(true);
        when(cardService.isCardOwnedByUser(2L, 1L)).thenReturn(false);

        String requestBody = objectMapper.writeValueAsString(new TransactionCreateRequest(
                "1234567890123456", "9876543210987654", BigDecimal.valueOf(100), "Test transaction"
        ));

        mockMvc.perform(post("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You can only transfer between your own cards"));
    }

    @Test
    void getUserTransactions_ShouldReturnUserTransactions() throws Exception {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionService.getUserTransactions(1L)).thenReturn(transactions);

        mockMvc.perform(get("/api/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(100));
    }

    @Test
    void getCardTransactions_ShouldReturnCardTransactions() throws Exception {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(true);
        when(transactionService.getCardTransactions(1L)).thenReturn(transactions);

        mockMvc.perform(get("/api/transactions/card/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getCardTransactions_ShouldReturnForbiddenWhenCardNotOwned() throws Exception {
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(false);

        mockMvc.perform(get("/api/transactions/card/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionById_ShouldReturnTransactionWhenUserIsParticipant() throws Exception {
        when(transactionService.getTransactionById(1L)).thenReturn(testTransaction);

        mockMvc.perform(get("/api/transactions/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void getTransactionById_ShouldReturnForbiddenWhenNotRelated() throws Exception {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .password("password")
                .role(User.Role.USER)
                .build();

        Card otherFromCard = Card.builder()
                .id(3L)
                .cardNumber("1111111111111111")
                .owner("Other Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .user(otherUser)
                .build();

        Card otherToCard = Card.builder()
                .id(4L)
                .cardNumber("2222222222222222")
                .owner("Other Owner 2")
                .expiryDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .user(otherUser)
                .build();

        Transaction otherTransaction = Transaction.builder()
                .id(2L)
                .fromCard(otherFromCard)
                .toCard(otherToCard)
                .amount(BigDecimal.valueOf(100))
                .description("Other transaction")
                .status(Transaction.TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        when(transactionService.getTransactionById(2L)).thenReturn(otherTransaction);

        mockMvc.perform(get("/api/transactions/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }
}
