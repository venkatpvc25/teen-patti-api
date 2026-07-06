package com.pvc.game.feature.economy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RewardResponse {
    private long chipsAwarded;
    private long balance;
    private String source;
}
