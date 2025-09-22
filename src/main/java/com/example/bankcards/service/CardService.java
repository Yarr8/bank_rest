package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardNumberException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public Card createCard(Card card, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (cardRepository.findByCardNumber(card.getCardNumber()).isPresent()) {
            throw new DuplicateCardNumberException(card.getCardNumber());
        }

        card.setUser(user);
        card.setStatus(Card.CardStatus.ACTIVE);

        return cardRepository.save(card);
    }


    public Card getCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    public Optional<Card> getCardByNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber);
    }

    public List<Card> getActiveUserCards(Long userId) {
        return cardRepository.findByUserIdAndStatus(userId, Card.CardStatus.ACTIVE);
    }


    @Transactional
    public Card topUpCard(Long cardId, BigDecimal amount) {
        Card card = getCardById(cardId);

        if (card.getStatus() != Card.CardStatus.ACTIVE) {
            throw new CardBlockedException("Cannot top up blocked or expired card");
        }

        card.setBalance(card.getBalance().add(amount));
        return cardRepository.save(card);
    }

    public boolean isCardOwnedByUser(Long cardId, Long userId) {
        Card card = getCardById(cardId);
        return card.getUser().getId().equals(userId);
    }

    @Transactional
    public Card blockCard(Long cardId) {
        log.info("Blocking card: {}", cardId);

        Card card = getCardById(cardId);
        card.setStatus(Card.CardStatus.BLOCKED);

        Card savedCard = cardRepository.save(card);
        log.info("Card blocked successfully: {}", cardId);

        return savedCard;
    }

    @Transactional
    public Card unblockCard(Long cardId) {
        log.info("Unblocking card: {}", cardId);

        Card card = getCardById(cardId);
        card.setStatus(Card.CardStatus.ACTIVE);

        Card savedCard = cardRepository.save(card);
        log.info("Card unblocked successfully: {}", cardId);

        return savedCard;
    }

    @Transactional
    public void deleteCard(Long cardId) {
        log.info("Deleting card: {}", cardId);

        Card card = getCardById(cardId);
        cardRepository.delete(card);

        log.info("Card deleted successfully: {}", cardId);
    }

    public BigDecimal getUserTotalBalance(Long userId) {
        log.info("Getting total balance for user: {}", userId);

        List<Card> userCards = cardRepository.findByUserId(userId);
        BigDecimal totalBalance = userCards.stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total balance for user {}: {}", userId, totalBalance);
        return totalBalance;
    }

    public List<Card> getUserCards(Long userId) {
        log.info("Getting cards for user: {}", userId);
        return cardRepository.findByUserId(userId);
    }

    public Page<Card> getUserCardsPaginated(Long userId, int page, int size, String sortBy, String sortDirection) {
        log.info("Getting paginated cards for user: {}, page: {}, size: {}", userId, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        return cardRepository.findByUserId(userId, pageable);
    }

    public Page<Card> getUserCardsPaginated(Long userId, int page, int size) {
        return getUserCardsPaginated(userId, page, size, "createdAt", "desc");
    }

    public List<Card> getAllCards() {
        log.info("Getting all cards");
        return cardRepository.findAll();
    }
}
