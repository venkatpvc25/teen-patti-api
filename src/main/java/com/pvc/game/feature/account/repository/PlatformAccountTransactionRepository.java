package com.pvc.game.feature.account.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.account.entity.PlatformAccountTransaction;

public interface PlatformAccountTransactionRepository extends JpaRepository<PlatformAccountTransaction, UUID> {
}
