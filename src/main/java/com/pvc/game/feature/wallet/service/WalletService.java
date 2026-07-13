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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    public static final long STARTING_CHIPS = 5_000;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public Wallet createStartingWallet(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating starting wallet userId={} startingChips={}", user.getId(), STARTING_CHIPS);
                    return credit(user, STARTING_CHIPS, "SIGNUP_BONUS", null);
                });
    }

    @Transactional
    public Wallet credit(User user, long amount, String source, String referenceId) {
        Wallet wallet = walletRepository.findByUser(user).orElseGet(() -> newWallet(user));
        long balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
        record(user, TransactionType.CREDIT, amount, wallet.getBalance(), source, referenceId);
        log.info("Wallet credited userId={} amount={} balanceBefore={} balanceAfter={} source={} referenceId={}",
                user.getId(), amount, balanceBefore, wallet.getBalance(), source, referenceId);
        return wallet;
    }

    @Transactional
    public Wallet debit(User user, long amount, String source, String referenceId) {
        Wallet wallet = walletRepository.findByUser(user).orElseGet(() -> newWallet(user));
        if (wallet.getBalance() < amount) {
            log.warn("Wallet debit rejected insufficientBalance userId={} amount={} balance={} source={} referenceId={}",
                    user.getId(), amount, wallet.getBalance(), source, referenceId);
            throw new IllegalStateException("Insufficient chips");
        }
        long balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
        record(user, TransactionType.DEBIT, amount, wallet.getBalance(), source, referenceId);
        log.info("Wallet debited userId={} amount={} balanceBefore={} balanceAfter={} source={} referenceId={}",
                user.getId(), amount, balanceBefore, wallet.getBalance(), source, referenceId);
        return wallet;
    }

    public Wallet getWallet(User user) {
        Wallet wallet = walletRepository.findByUser(user).orElseGet(() -> createStartingWallet(user));
        log.debug("Wallet fetched userId={} balance={}", user.getId(), wallet.getBalance());
        return wallet;
    }

    public List<WalletTransaction> recentTransactions(User user) {
        List<WalletTransaction> transactions = transactionRepository.findTop20ByUserOrderByCreatedAtDesc(user);
        log.debug("Recent wallet transactions fetched userId={} count={}", user.getId(), transactions.size());
        return transactions;
    }

    private Wallet newWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(0);
        log.info("New wallet entity initialized userId={}", user.getId());
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
