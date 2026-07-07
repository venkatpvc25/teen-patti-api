package com.pvc.game.feature.wallet.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.store.service.RazorpayService;
import com.pvc.game.feature.wallet.dto.WithdrawalRequest;
import com.pvc.game.feature.wallet.entity.TransactionType;
import com.pvc.game.feature.wallet.entity.Wallet;
import com.pvc.game.feature.wallet.entity.WalletTransaction;
import com.pvc.game.feature.wallet.entity.Withdrawal;
import com.pvc.game.feature.wallet.entity.WithdrawalStatus;
import com.pvc.game.feature.wallet.repository.WalletRepository;
import com.pvc.game.feature.wallet.repository.WalletTransactionRepository;
import com.pvc.game.feature.wallet.repository.WithdrawalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    public static final long STARTING_CHIPS = 5_000;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final RazorpayService razorpayService;

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

    public List<Withdrawal> recentWithdrawals(User user) {
        return withdrawalRepository.findTop20ByUserOrderByCreatedAtDesc(user);
    }

    public Withdrawal submitWithdrawal(User user, WithdrawalRequest request) {
        validateWithdrawalRequest(request);
        if (getWallet(user).getBalance() < request.getChips()) {
            throw new IllegalStateException("Insufficient chips");
        }

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUser(user);
        withdrawal.setChips(request.getChips());
        withdrawal.setAmountPaise(razorpayService.withdrawalPaiseFor(request.getChips()));
        withdrawal.setUpiId(normalize(request.getUpiId()));
        withdrawal.setRazorpayFundAccountId(normalize(request.getRazorpayFundAccountId()));
        withdrawalRepository.save(withdrawal);

        debit(user, withdrawal.getChips(), "WITHDRAWAL", withdrawal.getId().toString());
        try {
            ensureFundAccount(user, withdrawal);
            String payoutId = razorpayService.createPayout(
                    withdrawal.getRazorpayFundAccountId(),
                    withdrawal.getAmountPaise(),
                    withdrawal.getId().toString(),
                    "Game chips withdrawal");
            withdrawal.setRazorpayPayoutId(payoutId);
            withdrawal.setStatus(WithdrawalStatus.PROCESSING);
            withdrawal.setProcessedAt(Instant.now());
            return withdrawalRepository.save(withdrawal);
        } catch (RuntimeException exception) {
            credit(user, withdrawal.getChips(), "WITHDRAWAL_REFUND", withdrawal.getId().toString());
            withdrawal.setStatus(WithdrawalStatus.REFUNDED);
            withdrawal.setFailureReason(messageOf(exception));
            withdrawal.setProcessedAt(Instant.now());
            return withdrawalRepository.save(withdrawal);
        }
    }

    private Wallet newWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(0);
        return wallet;
    }

    private void ensureFundAccount(User user, Withdrawal withdrawal) {
        if (!isBlank(withdrawal.getRazorpayFundAccountId())) {
            return;
        }
        String contactId = razorpayService.createContact(
                user.getNickname(),
                user.getPhone(),
                user.getId().toString());
        String fundAccountId = razorpayService.createUpiFundAccount(contactId, withdrawal.getUpiId());
        withdrawal.setRazorpayContactId(contactId);
        withdrawal.setRazorpayFundAccountId(fundAccountId);
        withdrawalRepository.save(withdrawal);
    }

    private void validateWithdrawalRequest(WithdrawalRequest request) {
        if (request.getChips() == null || request.getChips() <= 0) {
            throw new IllegalArgumentException("Withdrawal chips must be greater than zero");
        }
        if (isBlank(request.getRazorpayFundAccountId()) && isBlank(request.getUpiId())) {
            throw new IllegalArgumentException("UPI ID or Razorpay fund account id is required");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String messageOf(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
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
