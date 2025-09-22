package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockRequestNotFoundException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.repository.CardBlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardBlockRequestServiceTest {

    @Mock
    private CardBlockRequestRepository cardBlockRequestRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardBlockRequestService cardBlockRequestService;

    private User testUser;
    private Card testCard;
    private CardBlockRequest testRequest;

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
                .status(Card.CardStatus.ACTIVE)
                .balance(java.math.BigDecimal.ZERO)
                .user(testUser)
                .build();

        testRequest = CardBlockRequest.builder()
                .id(1L)
                .card(testCard)
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createBlockRequest_ShouldCreateRequestSuccessfully() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardBlockRequestRepository.existsByCardIdAndStatus(1L, CardBlockRequest.RequestStatus.PENDING)).thenReturn(false);
        when(cardBlockRequestRepository.save(any(CardBlockRequest.class))).thenReturn(testRequest);

        CardBlockRequest result = cardBlockRequestService.createBlockRequest(1L, 1L, "Lost card");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Lost card", result.getReason());
        assertEquals(CardBlockRequest.RequestStatus.PENDING, result.getStatus());
        assertEquals(testCard, result.getCard());
        verify(cardBlockRequestRepository).save(any(CardBlockRequest.class));
    }

    @Test
    void createBlockRequest_ShouldThrowExceptionWhenCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () ->
                cardBlockRequestService.createBlockRequest(1L, 1L, "Lost card"));
    }

    @Test
    void createBlockRequest_ShouldThrowExceptionWhenPendingRequestExists() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardBlockRequestRepository.existsByCardIdAndStatus(1L, CardBlockRequest.RequestStatus.PENDING)).thenReturn(true);

        assertThrows(InvalidTransactionException.class, () ->
                cardBlockRequestService.createBlockRequest(1L, 1L, "Lost card"));
    }

    @Test
    void getUserRequests_ShouldReturnUserRequests() {
        List<CardBlockRequest> requests = Arrays.asList(testRequest);
        when(cardBlockRequestRepository.findByRequesterId(1L)).thenReturn(requests);

        List<CardBlockRequest> result = cardBlockRequestService.getUserRequests(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRequest, result.get(0));
    }

    @Test
    void getRequestsByStatus_ShouldReturnRequestsByStatus() {
        List<CardBlockRequest> requests = Arrays.asList(testRequest);
        when(cardBlockRequestRepository.findByStatus(CardBlockRequest.RequestStatus.PENDING)).thenReturn(requests);

        List<CardBlockRequest> result = cardBlockRequestService.getRequestsByStatus(CardBlockRequest.RequestStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRequest, result.get(0));
    }

    @Test
    void getAllRequests_ShouldReturnAllRequests() {
        List<CardBlockRequest> requests = Arrays.asList(testRequest);
        when(cardBlockRequestRepository.findAll()).thenReturn(requests);

        List<CardBlockRequest> result = cardBlockRequestService.getAllRequests();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRequest, result.get(0));
    }

    @Test
    void approveRequest_ShouldApproveRequestSuccessfully() {
        User adminUser = User.builder().id(2L).username("admin").role(User.Role.ADMIN).build();
        CardBlockRequest approvedRequest = CardBlockRequest.builder()
                .id(1L)
                .card(testCard)
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.APPROVED)
                .processedBy(adminUser)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardBlockRequestRepository.save(any(CardBlockRequest.class))).thenReturn(approvedRequest);

        CardBlockRequest result = cardBlockRequestService.approveRequest(1L, 2L);

        assertNotNull(result);
        assertEquals(CardBlockRequest.RequestStatus.APPROVED, result.getStatus());
        assertEquals(Card.CardStatus.BLOCKED, result.getCard().getStatus());
        assertEquals(2L, result.getProcessedBy().getId());
        assertNotNull(result.getProcessedAt());
        verify(cardRepository).save(any(Card.class));
        verify(cardBlockRequestRepository).save(any(CardBlockRequest.class));
    }

    @Test
    void approveRequest_ShouldThrowExceptionWhenRequestNotFound() {
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardBlockRequestNotFoundException.class, () ->
                cardBlockRequestService.approveRequest(1L, 2L));
    }

    @Test
    void approveRequest_ShouldThrowExceptionWhenNotPending() {
        testRequest.setStatus(CardBlockRequest.RequestStatus.APPROVED);
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        assertThrows(InvalidTransactionException.class, () ->
                cardBlockRequestService.approveRequest(1L, 2L));
    }

    @Test
    void rejectRequest_ShouldRejectRequestSuccessfully() {
        User adminUser = User.builder().id(2L).username("admin").role(User.Role.ADMIN).build();
        CardBlockRequest rejectedRequest = CardBlockRequest.builder()
                .id(1L)
                .card(testCard)
                .requester(testUser)
                .reason("Lost card")
                .status(CardBlockRequest.RequestStatus.REJECTED)
                .processedBy(adminUser)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(cardBlockRequestRepository.save(any(CardBlockRequest.class))).thenReturn(rejectedRequest);

        CardBlockRequest result = cardBlockRequestService.rejectRequest(1L, 2L);

        assertNotNull(result);
        assertEquals(CardBlockRequest.RequestStatus.REJECTED, result.getStatus());
        assertEquals(2L, result.getProcessedBy().getId());
        assertNotNull(result.getProcessedAt());
        verify(cardBlockRequestRepository).save(any(CardBlockRequest.class));
    }

    @Test
    void rejectRequest_ShouldThrowExceptionWhenRequestNotFound() {
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardBlockRequestNotFoundException.class, () ->
                cardBlockRequestService.rejectRequest(1L, 2L));
    }

    @Test
    void rejectRequest_ShouldThrowExceptionWhenNotPending() {
        testRequest.setStatus(CardBlockRequest.RequestStatus.REJECTED);
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        assertThrows(InvalidTransactionException.class, () ->
                cardBlockRequestService.rejectRequest(1L, 2L));
    }

    @Test
    void getRequestById_ShouldReturnRequestWhenFound() {
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        CardBlockRequest result = cardBlockRequestService.getRequestById(1L);

        assertNotNull(result);
        assertEquals(testRequest, result);
    }

    @Test
    void getRequestById_ShouldThrowExceptionWhenNotFound() {
        when(cardBlockRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardBlockRequestNotFoundException.class, () ->
                cardBlockRequestService.getRequestById(1L));
    }
}
