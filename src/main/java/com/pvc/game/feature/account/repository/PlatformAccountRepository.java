package com.pvc.game.feature.account.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.account.entity.PlatformAccount;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, UUID> {
    Optional<PlatformAccount> findByCode(String code);
}
