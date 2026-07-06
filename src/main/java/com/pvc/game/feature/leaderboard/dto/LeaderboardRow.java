package com.pvc.game.feature.leaderboard.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardRow {
    private UUID userId;
    private String nickname;
    private long xp;
    private int level;
}
