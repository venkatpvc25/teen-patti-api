package com.pvc.game.feature.wallet.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.wallet.entity.TransactionType;
import com.pvc.game.feature.wallet.entity.Wallet;
import com.pvc.game.feature.wallet.entity.WalletTransaction;
import com.pvc.game.feature.wallet.repository.WalletRepository;
import com.pvc.game.feature.wallet.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    public static final long STARTING_CHIPS = 5_000;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public Wallet createStartingWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> credit(user, STARTING_CHIPS, "SIGNUP_BONUS", null));
    }

    @Transactional
    public Wallet credit(User user, long amount, String source, String referenceId) {
        Wallet wallet = walletRepository.findByUser(user).orElseGet(() -> newWallet(user));
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
        record(user, TransactionType.CREDIT, amount, wallet.getBalance(), source, referenceId);
        return wallet;
    }

    @Transactional
    public Wallet debit(User user, long amount, String source, String referenceId) {
        Wallet wallet = walletRepository.findByUser(user).orElseGet(() -> newWallet(user));
        if (wallet.getBalance() < amount) {
            throw new IllegalStateException("Insufficient chips");
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
        record(user, TransactionType.DEBIT, amount, wallet.getBalance(), source, referenceId);
        return wallet;
    }

    public Wallet getWallet(User user) {
        return walletRepository.findByUser(user).orElseGet(() -> createStartingWallet(user));
    }

    public List<WalletTransaction> recentTransactions(User user) {
        return transactionRepository.findTop20ByUserOrderByCreatedAtDesc(user);
    }

    private Wallet newWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(0);
        return wallet;
    }

    private void record(User user, TransactionType type, long amount, long balanceAfter, String source, String referenceId) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setSource(source);
        tx.setReferenceId(referenceId);
        transactionRepository.save(tx);
    }
}
