package com.pvc.game.feature.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.store.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    Optional<Purchase> findByProviderOrderId(String providerOrderId);
    boolean existsByProviderPaymentId(String providerPaymentId);
}
