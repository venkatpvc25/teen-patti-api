package com.pvc.game.feature.wallet.entity;

import java.time.Instant;
import java.util.UUID;

import com.pvc.game.feature.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "withdrawals")
@Getter
@Setter
public class Withdrawal {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(nullable = false)
    private long chips;

    @Column(nullable = false)
    private long amountPaise;

    @Column(nullable = false)
    private String currency = "INR";

    @Column(nullable = false)
    private String payoutMode = "UPI";

    private String upiId;
    private String razorpayContactId;
    private String razorpayFundAccountId;
    private String razorpayPayoutId;
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant processedAt;
}
