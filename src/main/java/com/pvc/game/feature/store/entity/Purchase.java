package com.pvc.game.feature.store.entity;

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
@Table(name = "purchases")
@Getter
@Setter
public class Purchase {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    private ShopItem shopItem;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String paymentProvider = "RAZORPAY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @Column(nullable = false)
    private long amountPaise;

    @Column(nullable = false)
    private String currency;

    @Column(unique = true)
    private String providerOrderId;

    @Column(unique = true)
    private String providerPaymentId;

    private String receiptReference;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant paidAt;
}
