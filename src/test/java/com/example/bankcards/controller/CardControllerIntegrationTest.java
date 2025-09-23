package com.example.bankcards.controller;

import com.example.bankcards.dto.CardBlockRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.util.CardMasker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.bankcards.security.JwtUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private CardBlockRequestService cardBlockRequestService;

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
    private Card testCard;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(User.Role.USER)
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .owner("Test Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(testUser)
                .build();

        UserDetails testUserDetails = new JwtUser(testUser);
        jwtToken = jwtUtil.generateToken(testUserDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void getUserCards_ShouldReturnPaginatedUserCards() throws Exception {
        List<Card> cards = Arrays.asList(testCard);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(0, 20), 1);
        when(cardService.getUserCardsPaginated(anyLong(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(cardPage);
        String maskedCardNumber = CardMasker.maskCardNumber(testCard.getCardNumber());

        mockMvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].cardNumber").value(maskedCardNumber))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getCard_ShouldReturnCardWhenFound() throws Exception {
        when(cardService.getCardById(1L)).thenReturn(testCard);
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(true);
        String maskedCardNumber = CardMasker.maskCardNumber(testCard.getCardNumber());

        mockMvc.perform(get("/api/cards/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardNumber").value(maskedCardNumber));
    }

    @Test
    void getCard_ShouldReturnForbiddenWhenNotOwned() throws Exception {
        when(cardService.getCardById(1L)).thenReturn(testCard);
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(false);

        mockMvc.perform(get("/api/cards/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestCardBlock_ShouldCreateBlockRequestSuccessfully() throws Exception {
        CardBlockRequest blockRequest = CardBlockRequest.builder()
                .id(1L)
                .card(testCard)
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(true);
        when(cardBlockRequestService.createBlockRequest(1L, 1L, "Lost card")).thenReturn(blockRequest);

        String requestBody = objectMapper.writeValueAsString(new CardBlockRequestDto("Lost card"));

        mockMvc.perform(post("/api/cards/1/request-block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardId").value(1))
                .andExpect(jsonPath("$.reason").value("Lost card"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void requestCardBlock_ShouldReturnForbiddenWhenNotOwned() throws Exception {
        when(cardService.isCardOwnedByUser(1L, 1L)).thenReturn(false);

        String requestBody = objectMapper.writeValueAsString(new CardBlockRequestDto("Lost card"));

        mockMvc.perform(post("/api/cards/1/request-block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void getUserBalance_ShouldReturnUserBalance() throws Exception {
        BigDecimal totalBalance = BigDecimal.valueOf(100);
        List<Card> userCards = Arrays.asList(testCard);
        when(cardService.getUserTotalBalance(1L)).thenReturn(totalBalance);
        when(cardService.getUserCards(1L)).thenReturn(userCards);

        mockMvc.perform(get("/api/cards/balance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(100))
                .andExpect(jsonPath("$.cardBalances").isArray())
                .andExpect(jsonPath("$.cardBalances[0].cardId").value(1))
                .andExpect(jsonPath("$.cardBalances[0].balance").value(0))
                .andExpect(jsonPath("$.cardBalances[0].status").value("ACTIVE"));
    }
}
