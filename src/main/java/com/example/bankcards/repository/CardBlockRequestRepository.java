package com.example.bankcards.repository;

import com.example.bankcards.entity.CardBlockRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardBlockRequestRepository extends JpaRepository<CardBlockRequest, Long> {
    List<CardBlockRequest> findByCardId(Long cardId);

    List<CardBlockRequest> findByRequesterId(Long requesterId);

    List<CardBlockRequest> findByStatus(CardBlockRequest.RequestStatus status);

    Optional<CardBlockRequest> findByCardIdAndStatus(Long cardId, CardBlockRequest.RequestStatus status);

    boolean existsByCardIdAndStatus(Long cardId, CardBlockRequest.RequestStatus status);
}
