package com.pvc.game.feature.account.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.game.feature.account.entity.PlatformAccount;
import com.pvc.game.feature.account.entity.PlatformAccountTransaction;
import com.pvc.game.feature.account.repository.PlatformAccountRepository;
import com.pvc.game.feature.account.repository.PlatformAccountTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlatformAccountService {

    private static final String HOUSE_ACCOUNT_CODE = "HOUSE";

    private final PlatformAccountRepository accountRepository;
    private final PlatformAccountTransactionRepository transactionRepository;

    @Transactional
    public PlatformAccount creditHouse(long amount, String source, String referenceId, UUID userId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        PlatformAccount account = accountRepository.findByCode(HOUSE_ACCOUNT_CODE)
                .orElseGet(this::newHouseAccount);
        account.setBalance(account.getBalance() + amount);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        record(account, amount, source, referenceId, userId);
        return account;
    }

    private PlatformAccount newHouseAccount() {
        PlatformAccount account = new PlatformAccount();
        account.setCode(HOUSE_ACCOUNT_CODE);
        account.setBalance(0);
        return account;
    }

    private void record(PlatformAccount account, long amount, String source, String referenceId, UUID userId) {
        PlatformAccountTransaction tx = new PlatformAccountTransaction();
        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setBalanceAfter(account.getBalance());
        tx.setSource(source);
        tx.setReferenceId(referenceId);
        tx.setUserId(userId);
        transactionRepository.save(tx);
    }
}
