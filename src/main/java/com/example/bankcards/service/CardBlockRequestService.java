package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockRequestNotFoundException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.repository.CardBlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardBlockRequestService {

    private final CardBlockRequestRepository cardBlockRequestRepository;
    private final CardRepository cardRepository;

    @Transactional
    public CardBlockRequest createBlockRequest(Long cardId, Long requesterId, String reason) {
        log.info("Creating block request for card: {} by user: {}", cardId, requesterId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (cardBlockRequestRepository.existsByCardIdAndStatus(cardId, CardBlockRequest.RequestStatus.PENDING)) {
            throw new InvalidTransactionException("There is already a pending block request for this card");
        }

        CardBlockRequest request = CardBlockRequest.builder()
                .card(card)
                .requester(User.builder().id(requesterId).build())
                .reason(reason)
                .status(CardBlockRequest.RequestStatus.PENDING)
                .build();

        CardBlockRequest savedRequest = cardBlockRequestRepository.save(request);
        log.info("Block request created successfully with id: {}", savedRequest.getId());

        return savedRequest;
    }

    public List<CardBlockRequest> getUserRequests(Long userId) {
        log.info("Getting block requests for user: {}", userId);
        return cardBlockRequestRepository.findByRequesterId(userId);
    }

    public List<CardBlockRequest> getRequestsByStatus(CardBlockRequest.RequestStatus status) {
        log.info("Getting block requests by status: {}", status);
        return cardBlockRequestRepository.findByStatus(status);
    }

    public List<CardBlockRequest> getAllRequests() {
        log.info("Getting all block requests");
        return cardBlockRequestRepository.findAll();
    }

    @Transactional
    public CardBlockRequest approveRequest(Long requestId, Long adminId) {
        log.info("Approving block request: {} by admin: {}", requestId, adminId);

        CardBlockRequest request = cardBlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new CardBlockRequestNotFoundException(requestId));

        if (request.getStatus() != CardBlockRequest.RequestStatus.PENDING) {
            throw new InvalidTransactionException("Request is not in PENDING status");
        }

        // block card
        request.getCard().setStatus(Card.CardStatus.BLOCKED);
        cardRepository.save(request.getCard());
        // and approve request
        request.setStatus(CardBlockRequest.RequestStatus.APPROVED);
        request.setProcessedBy(User.builder().id(adminId).build());
        request.setProcessedAt(LocalDateTime.now());

        CardBlockRequest savedRequest = cardBlockRequestRepository.save(request);
        log.info("Block request approved successfully: {}", requestId);

        return savedRequest;
    }

    @Transactional
    public CardBlockRequest rejectRequest(Long requestId, Long adminId) {
        log.info("Rejecting block request: {} by admin: {}", requestId, adminId);

        CardBlockRequest request = cardBlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new CardBlockRequestNotFoundException(requestId));

        if (request.getStatus() != CardBlockRequest.RequestStatus.PENDING) {
            throw new InvalidTransactionException("Request is not in PENDING status");
        }

        request.setStatus(CardBlockRequest.RequestStatus.REJECTED);
        request.setProcessedBy(User.builder().id(adminId).build());
        request.setProcessedAt(LocalDateTime.now());

        CardBlockRequest savedRequest = cardBlockRequestRepository.save(request);
        log.info("Block request rejected successfully: {}", requestId);

        return savedRequest;
    }

    public CardBlockRequest getRequestById(Long id) {
        log.info("Getting block request by id: {}", id);
        return cardBlockRequestRepository.findById(id)
                .orElseThrow(() -> new CardBlockRequestNotFoundException(id));
    }
}
