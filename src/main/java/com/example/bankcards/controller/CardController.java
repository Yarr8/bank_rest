package com.example.bankcards.controller;

import com.example.bankcards.dto.CardBlockRequestDto;
import com.example.bankcards.dto.CardBlockRequestResponse;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardTopUpRequest;
import com.example.bankcards.dto.GenericErrorResponse;
import com.example.bankcards.dto.PaginatedResponse;
import com.example.bankcards.dto.UserBalanceResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtUser;
import com.example.bankcards.service.CardBlockRequestService;
import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.bankcards.util.CardMasker;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;
    private final CardBlockRequestService cardBlockRequestService;


    @GetMapping
    public ResponseEntity<?> getUserCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication) {
        log.info("Getting paginated cards for user: {}, page: {}, size: {}",
                authentication.getName(), page, size);

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            // validate parameters
            if (page < 0) page = 0;
            if (size < 1 || size > 100) size = 20;

            Page<Card> cardPage = cardService.getUserCardsPaginated(
                    currentUser.getId(), page, size, sortBy, sortDirection);

            List<CardResponse> cardResponses = cardPage.getContent().stream()
                    .map(CardResponse::new)
                    .toList();

            PaginatedResponse<CardResponse> response = new PaginatedResponse<>(
                    cardResponses,
                    cardPage.getNumber(),
                    cardPage.getSize(),
                    cardPage.getTotalElements(),
                    cardPage.getTotalPages(),
                    cardPage.isFirst(),
                    cardPage.isLast(),
                    cardPage.hasNext(),
                    cardPage.hasPrevious()
            );

            log.info("Retrieved {} cards for user: {} (page {}/{})",
                    cardResponses.size(), authentication.getName(),
                    page + 1, cardPage.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting user cards: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get cards: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCard(@PathVariable Long id, Authentication authentication) {
        log.info("Getting card: {} for user: {}", id, authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();
            Card card = cardService.getCardById(id);

            if (!cardService.isCardOwnedByUser(id, currentUser.getId())) {
                return ResponseEntity.status(403)
                        .body(new GenericErrorResponse("Access denied"));
            }

            return ResponseEntity.ok(new CardResponse(card));

        } catch (Exception e) {
            log.error("Error getting card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get card: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/topup")
    public ResponseEntity<?> topUpCard(@PathVariable Long id,
                                       @Valid @RequestBody CardTopUpRequest request,
                                       Authentication authentication) {
        log.info("Topping up card: {} with amount: {} for user: {}",
                id, request.getAmount(), authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            if (!cardService.isCardOwnedByUser(id, currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Card not found or access denied"));
            }

            Card toppedUpCard = cardService.topUpCard(id, request.getAmount());

            log.info("Card topped up successfully: {} new balance: {}",
                    id, toppedUpCard.getBalance());

            return ResponseEntity.ok(new CardResponse(toppedUpCard));

        } catch (Exception e) {
            log.error("Error topping up card: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to top up card: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/request-block")
    public ResponseEntity<?> requestCardBlock(@PathVariable Long id,
                                              @Valid @RequestBody CardBlockRequestDto request,
                                              Authentication authentication) {
        log.info("User {} requesting block for card: {}", authentication.getName(), id);

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            if (!cardService.isCardOwnedByUser(id, currentUser.getId())) {
                log.warn("User {} attempted to request block for card {} not owned by them",
                        authentication.getName(), id);
                return ResponseEntity.status(403)
                        .body(new GenericErrorResponse("Access denied"));
            }

            CardBlockRequest blockRequest = cardBlockRequestService.createBlockRequest(
                    id, currentUser.getId(), request.getReason());

            log.info("Block request created successfully with id: {}", blockRequest.getId());

            return ResponseEntity.ok(new CardBlockRequestResponse(blockRequest));

        } catch (Exception e) {
            log.error("Error creating block request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to create block request: " + e.getMessage()));
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getUserBalance(Authentication authentication) {
        log.info("Getting balance for user: {}", authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            BigDecimal totalBalance = cardService.getUserTotalBalance(currentUser.getId());
            List<Card> userCards = cardService.getUserCards(currentUser.getId());
            List<UserBalanceResponse.CardBalance> cardBalances = userCards.stream()
                    .map(card -> new UserBalanceResponse.CardBalance(
                            card.getId(),
                            CardMasker.maskCardNumber(card.getCardNumber()),
                            card.getBalance(),
                            card.getStatus().name()
                    ))
                    .toList();

            UserBalanceResponse response = new UserBalanceResponse(totalBalance, cardBalances);

            log.info("Balance retrieved successfully for user: {}", authentication.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting user balance: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get user balance: " + e.getMessage()));
        }
    }
}
