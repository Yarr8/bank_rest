package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

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
                .cardNumber("1234567890123456")
                .owner("Test Owner")
                .expiryDate(LocalDate.now().plusYears(1))
                .build();
    }

    @Test
    void createCard_ShouldCreateCardSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.createCard(testCard, 1L);

        assertNotNull(result);
        assertEquals(Card.CardStatus.ACTIVE, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals(testUser, result.getUser());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_ShouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                cardService.createCard(testCard, 999L));
    }

    @Test
    void blockCard_ShouldBlockCardSuccessfully() {
        testCard.setStatus(Card.CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.blockCard(1L);

        assertEquals(Card.CardStatus.BLOCKED, result.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void unblockCard_ShouldUnblockCardSuccessfully() {
        testCard.setStatus(Card.CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.unblockCard(1L);

        assertEquals(Card.CardStatus.ACTIVE, result.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void isCardOwnedByUser_ShouldReturnTrueWhenCardIsOwned() {
        testCard.setUser(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        boolean result = cardService.isCardOwnedByUser(1L, 1L);

        assertTrue(result);
    }

    @Test
    void isCardOwnedByUser_ShouldReturnFalseWhenCardIsNotOwned() {
        User otherUser = User.builder().id(2L).build();
        testCard.setUser(otherUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        boolean result = cardService.isCardOwnedByUser(1L, 1L);

        assertFalse(result);
    }
}
