package com.pvc.game.feature.wallet.dto;

import java.util.List;
import java.util.UUID;

import com.pvc.game.feature.wallet.entity.WalletTransaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletResponse {
    private long balance;
    private List<TransactionLine> recentTransactions;

    @Data
    @AllArgsConstructor
    public static class TransactionLine {
        private UUID id;
        private String type;
        private long amount;
        private long balanceAfter;
        private String source;
        private String referenceId;

        public static TransactionLine from(WalletTransaction tx) {
            return new TransactionLine(
                    tx.getId(),
                    tx.getType().name(),
                    tx.getAmount(),
                    tx.getBalanceAfter(),
                    tx.getSource(),
                    tx.getReferenceId());
        }
    }
}
