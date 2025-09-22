package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.exception.TransactionNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.util.CardMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creating transaction from card {} to card {} amount: {}",
                CardMasker.maskCardNumber(transaction.getFromCard().getCardNumber()),
                CardMasker.maskCardNumber(transaction.getToCard().getCardNumber()),
                transaction.getAmount());

        validateTransaction(transaction);

        if (!transaction.getFromCard().getUser().getId().equals(transaction.getToCard().getUser().getId())) {
            throw new InvalidTransactionException("Transaction between cards of different users is not allowed");
        }

        if (transaction.getFromCard().getBalance().compareTo(transaction.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    transaction.getAmount(),
                    transaction.getFromCard().getBalance()
            );
        }

        if (transaction.getFromCard().getStatus() != Card.CardStatus.ACTIVE) {
            throw new CardBlockedException("Source card is not active");
        }

        if (transaction.getToCard().getStatus() != Card.CardStatus.ACTIVE) {
            throw new CardBlockedException("Destination card is not active");
        }

        try {
            transaction.getFromCard().setBalance(
                    transaction.getFromCard().getBalance().subtract(transaction.getAmount())
            );
            transaction.getToCard().setBalance(
                    transaction.getToCard().getBalance().add(transaction.getAmount())
            );

            cardRepository.save(transaction.getFromCard());
            cardRepository.save(transaction.getToCard());

            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setProcessedAt(LocalDateTime.now());

            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Transaction completed successfully with id: {}", savedTransaction.getId());

            return savedTransaction;

        } catch (Exception e) {
            log.error("Error processing transaction: {}", e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            return transactionRepository.save(transaction);
        }
    }

    public List<Transaction> getUserTransactions(Long userId) {
        log.info("Getting transactions for user: {}", userId);
        return transactionRepository.findByFromCardUserIdOrToCardUserId(userId, userId);
    }

    public List<Transaction> getCardTransactions(Long cardId) {
        log.info("Getting transactions for card: {}", cardId);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        return transactionRepository.findByFromCardOrToCard(card, card);
    }

    public Transaction getTransactionById(Long id) {
        log.info("Getting transaction by id: {}", id);
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional
    public Transaction cancelTransaction(Long transactionId) {
        log.info("Cancelling transaction: {}", transactionId);

        Transaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new InvalidTransactionException("Cannot cancel transaction in status: " + transaction.getStatus());
        }

        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction cancelled successfully: {}", transactionId);

        return savedTransaction;
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be positive");
        }

        if (transaction.getFromCard().getId().equals(transaction.getToCard().getId())) {
            throw new InvalidTransactionException("Cannot transfer to the same card");
        }

        if (transaction.getFromCard().getUser().getId().equals(transaction.getToCard().getUser().getId())) {
            log.info("Internal transfer between user's own cards");
        }
    }

    public List<Transaction> getAllTransactions() {
        log.info("Getting all transactions");
        return transactionRepository.findAll();
    }
}
