package com.pvc.game.feature.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardResponse {
    private long handsPlayed;
    private long handsPlayedToday;
    private double winRate;
    private double winRateWeeklyChange;
    private int currentWinStreak;
    private int bestWinStreak;
    private long bestHandWon;
    private String bestHandRank;
    private List<String> bestHandCards;
    private List<DashboardMetric> metrics;
}
