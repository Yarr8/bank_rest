package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.exception.TransactionNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private TransactionService transactionService;

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
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .user(testUser)
                .build();

        toCard = Card.builder()
                .id(2L)
                .cardNumber("9876543210987654")
                .owner("Test Owner 2")
                .expiryDate(LocalDate.now().plusYears(1))
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
                .status(Transaction.TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createTransaction_ShouldCompleteSuccessfully() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(cardRepository.save(any(Card.class))).thenReturn(fromCard, toCard);

        Transaction result = transactionService.createTransaction(testTransaction);

        assertNotNull(result);
        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getProcessedAt());
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    void createTransaction_ShouldThrowExceptionWhenInsufficientFunds() {
        testTransaction.setAmount(BigDecimal.valueOf(2000));

        assertThrows(InsufficientFundsException.class, () ->
                transactionService.createTransaction(testTransaction));
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_ShouldThrowExceptionWhenSourceCardInactive() {
        fromCard.setStatus(Card.CardStatus.BLOCKED);

        assertThrows(CardBlockedException.class, () ->
                transactionService.createTransaction(testTransaction));
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_ShouldThrowExceptionWhenDestinationCardInactive() {
        toCard.setStatus(Card.CardStatus.BLOCKED);

        assertThrows(CardBlockedException.class, () ->
                transactionService.createTransaction(testTransaction));
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_ShouldThrowExceptionWhenAmountIsZero() {
        testTransaction.setAmount(BigDecimal.ZERO);

        assertThrows(InvalidTransactionException.class, () -> transactionService.createTransaction(testTransaction));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_ShouldThrowExceptionWhenSameCard() {
        testTransaction.setToCard(fromCard);

        assertThrows(InvalidTransactionException.class, () -> transactionService.createTransaction(testTransaction));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getUserTransactions_ShouldReturnUserTransactions() {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByFromCardUserIdOrToCardUserId(1L, 1L)).thenReturn(transactions);

        List<Transaction> result = transactionService.getUserTransactions(1L);

        assertEquals(1, result.size());
        assertEquals(testTransaction.getId(), result.get(0).getId());
    }

    @Test
    void getCardTransactions_ShouldReturnCardTransactions() {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(transactionRepository.findByFromCardOrToCard(fromCard, fromCard)).thenReturn(transactions);

        List<Transaction> result = transactionService.getCardTransactions(1L);

        assertEquals(1, result.size());
        assertEquals(testTransaction.getId(), result.get(0).getId());
    }

    @Test
    void getCardTransactions_ShouldThrowExceptionWhenCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> transactionService.getCardTransactions(1L));
    }

    @Test
    void getTransactionById_ShouldReturnTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        Transaction result = transactionService.getTransactionById(1L);

        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());
    }

    @Test
    void getTransactionById_ShouldThrowExceptionWhenNotFound() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById(1L));
    }
}
