package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.bankcards.util.CardNumberAttributeConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "cardNumber")
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Card number is required")
    @Size(min = 16, max = 16, message = "Card number must be exactly 16 digits")
    @Column(name = "card_number", nullable = false, unique = true)
    @Convert(converter = CardNumberAttributeConverter.class)
    private String cardNumber;
    
    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    @Column(nullable = false)
    private String owner;
    
    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;
    
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CardStatus {
        ACTIVE, BLOCKED, EXPIRED
    }
}