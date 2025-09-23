package com.example.bankcards.controller;

import com.example.bankcards.dto.TransactionCreateRequest;
import com.example.bankcards.dto.TransactionResponse;
import com.example.bankcards.dto.GenericErrorResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtUser;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.util.CardMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final CardService cardService;

    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody TransactionCreateRequest request,
                                               Authentication authentication) {
        log.info("Creating transaction from card {} to card {} amount: {} for user: {}",
                CardMasker.maskCardNumber(request.getFromCardNumber()),
                CardMasker.maskCardNumber(request.getToCardNumber()),
                request.getAmount(),
                authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            Optional<Card> fromCardOpt = cardService.getCardByNumber(request.getFromCardNumber());
            Optional<Card> toCardOpt = cardService.getCardByNumber(request.getToCardNumber());

            if (fromCardOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Source card not found"));
            }

            if (toCardOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Destination card not found"));
            }

            Card fromCard = fromCardOpt.get();
            Card toCard = toCardOpt.get();

            if (!cardService.isCardOwnedByUser(fromCard.getId(), currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("Access denied to source card"));
            }

            if (!cardService.isCardOwnedByUser(toCard.getId(), currentUser.getId())) {
                return ResponseEntity.badRequest()
                        .body(new GenericErrorResponse("You can only transfer between your own cards"));
            }

            Transaction transaction = Transaction.builder()
                    .fromCard(fromCard)
                    .toCard(toCard)
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .build();

            Transaction savedTransaction = transactionService.createTransaction(transaction);

            log.info("Transaction created successfully with id: {}", savedTransaction.getId());

            return ResponseEntity.ok(new TransactionResponse(savedTransaction));

        } catch (Exception e) {
            log.error("Error creating transaction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to create transaction: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserTransactions(Authentication authentication) {
        log.info("Getting transactions for user: {}", authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();
            List<Transaction> transactions = transactionService.getUserTransactions(currentUser.getId());

            List<TransactionResponse> transactionResponses = transactions.stream()
                    .map(TransactionResponse::new)
                    .toList();

            return ResponseEntity.ok(transactionResponses);

        } catch (Exception e) {
            log.error("Error getting user transactions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<?> getCardTransactions(@PathVariable Long cardId,
                                                 Authentication authentication) {
        log.info("Getting transactions for card: {} for user: {}", cardId, authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();

            if (!cardService.isCardOwnedByUser(cardId, currentUser.getId())) {
                return ResponseEntity.status(403)
                        .body(new GenericErrorResponse("Access denied"));
            }

            List<Transaction> transactions = transactionService.getCardTransactions(cardId);

            List<TransactionResponse> transactionResponses = transactions.stream()
                    .map(TransactionResponse::new)
                    .toList();

            return ResponseEntity.ok(transactionResponses);

        } catch (Exception e) {
            log.error("Error getting card transactions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get card transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id,
                                            Authentication authentication) {
        log.info("Getting transaction: {} for user: {}", id, authentication.getName());

        try {
            JwtUser userDetails = (JwtUser) authentication.getPrincipal();
            User currentUser = userDetails.getUser();
            Transaction transaction = transactionService.getTransactionById(id);

            boolean isParticipant = transaction.getFromCard().getUser().getId().equals(currentUser.getId()) ||
                    transaction.getToCard().getUser().getId().equals(currentUser.getId());

            if (!isParticipant) {
                return ResponseEntity.status(403)
                        .body(new GenericErrorResponse("Access denied"));
            }

            return ResponseEntity.ok(new TransactionResponse(transaction));

        } catch (Exception e) {
            log.error("Error getting transaction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new GenericErrorResponse("Failed to get transaction: " + e.getMessage()));
        }
    }
}
