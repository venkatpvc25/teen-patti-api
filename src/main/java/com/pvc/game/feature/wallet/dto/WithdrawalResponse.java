package com.pvc.game.feature.wallet.dto;

import java.time.Instant;
import java.util.UUID;

import com.pvc.game.feature.wallet.entity.Withdrawal;
import com.pvc.game.feature.wallet.entity.WithdrawalStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WithdrawalResponse {
    private UUID id;
    private WithdrawalStatus status;
    private long chips;
    private long amountPaise;
    private String currency;
    private String payoutMode;
    private String upiId;
    private String razorpayFundAccountId;
    private String razorpayPayoutId;
    private String failureReason;
    private Instant createdAt;
    private Instant processedAt;

    public static WithdrawalResponse from(Withdrawal withdrawal) {
        return new WithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getStatus(),
                withdrawal.getChips(),
                withdrawal.getAmountPaise(),
                withdrawal.getCurrency(),
                withdrawal.getPayoutMode(),
                withdrawal.getUpiId(),
                withdrawal.getRazorpayFundAccountId(),
                withdrawal.getRazorpayPayoutId(),
                withdrawal.getFailureReason(),
                withdrawal.getCreatedAt(),
                withdrawal.getProcessedAt());
    }
}
