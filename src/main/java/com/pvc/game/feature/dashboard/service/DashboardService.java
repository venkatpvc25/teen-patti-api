package com.pvc.game.feature.dashboard.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.dashboard.dto.DashboardMetric;
import com.pvc.game.feature.dashboard.dto.DashboardResponse;
import com.pvc.game.feature.game.entity.MatchPlayer;
import com.pvc.game.feature.game.entity.PlayerResult;
import com.pvc.game.feature.game.repository.MatchPlayerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<PlayerResult> COMPLETED_RESULTS = List.of(PlayerResult.WON, PlayerResult.LOST);

    private final MatchPlayerRepository matchPlayerRepository;
    private final ObjectMapper objectMapper;

    public DashboardResponse dashboardFor(User user) {
        ZoneId zone = ZoneId.systemDefault();
        Instant todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        LocalDate today = LocalDate.now(zone);
        Instant weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay(zone)
                .toInstant();
        Instant previousWeekStart = today.minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay(zone)
                .toInstant();

        long handsPlayed = matchPlayerRepository.countByUserAndResultIn(user, COMPLETED_RESULTS);
        long wins = matchPlayerRepository.countByUserAndResult(user, PlayerResult.WON);
        long handsPlayedToday = matchPlayerRepository.countFinishedSince(user, COMPLETED_RESULTS, todayStart);
        long currentWeekPlayed = matchPlayerRepository.countFinishedSince(user, COMPLETED_RESULTS, weekStart);
        long currentWeekWins = matchPlayerRepository.countByResultSince(user, PlayerResult.WON, weekStart);
        long previousWeekPlayed = matchPlayerRepository.countFinishedSince(user, COMPLETED_RESULTS, previousWeekStart)
                - currentWeekPlayed;
        long previousWeekWins = matchPlayerRepository.countByResultSince(user, PlayerResult.WON, previousWeekStart)
                - currentWeekWins;

        double winRate = percent(wins, handsPlayed);
        double weeklyChange = percent(currentWeekWins, currentWeekPlayed) - percent(previousWeekWins, previousWeekPlayed);

        List<MatchPlayer> finishedHands = matchPlayerRepository.findFinishedByUserOrderByFinishedAtDesc(
                user,
                COMPLETED_RESULTS);
        int currentStreak = currentWinStreak(finishedHands);
        int bestStreak = bestWinStreak(finishedHands);
        BestHand bestHand = bestHand(user, finishedHands).orElse(BestHand.empty());

        return new DashboardResponse(
                handsPlayed,
                handsPlayedToday,
                winRate,
                weeklyChange,
                currentStreak,
                bestStreak,
                bestHand.amountWon(),
                bestHand.rankLabel(),
                bestHand.cards(),
                metrics(handsPlayed, handsPlayedToday, winRate, weeklyChange, currentStreak, bestStreak, bestHand));
    }

    private List<DashboardMetric> metrics(
            long handsPlayed,
            long handsPlayedToday,
            double winRate,
            double weeklyChange,
            int currentStreak,
            int bestStreak,
            BestHand bestHand) {
        return List.of(
                new DashboardMetric(
                        "handsPlayed",
                        "playing_card",
                        "Hands Played",
                        formatNumber(handsPlayed),
                        "HANDS PLAYED",
                        "+" + formatNumber(handsPlayedToday) + " today",
                        handsPlayedToday > 0 ? "up" : "flat",
                        handsPlayed),
                new DashboardMetric(
                        "winRate",
                        "chart_up",
                        "Win Rate",
                        formatPercent(winRate),
                        "WIN RATE",
                        formatSignedPercent(weeklyChange) + " this week",
                        weeklyChange > 0 ? "up" : weeklyChange < 0 ? "down" : "flat",
                        Math.round(winRate * 10)),
                new DashboardMetric(
                        "currentStreak",
                        "fire",
                        "Current Streak",
                        currentStreak + (currentStreak == 1 ? " Win" : " Wins"),
                        "CURRENT STREAK",
                        currentStreak >= bestStreak && currentStreak > 0 ? "Personal best" : "Best " + bestStreak,
                        currentStreak > 0 ? "up" : "flat",
                        currentStreak),
                new DashboardMetric(
                        "bestHandWon",
                        "crown",
                        "Best Hand Won",
                        "₹" + formatNumber(bestHand.amountWon()),
                        "BEST HAND WON",
                        bestHand.rankLabel(),
                        bestHand.amountWon() > 0 ? "up" : "flat",
                        bestHand.amountWon()));
    }

    private int currentWinStreak(List<MatchPlayer> hands) {
        int streak = 0;
        for (MatchPlayer hand : hands) {
            if (hand.getResult() != PlayerResult.WON) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int bestWinStreak(List<MatchPlayer> hands) {
        int best = 0;
        int current = 0;
        List<MatchPlayer> oldestFirst = hands.stream()
                .sorted(Comparator.comparing(hand -> hand.getMatch().getFinishedAt()))
                .toList();
        for (MatchPlayer hand : oldestFirst) {
            if (hand.getResult() == PlayerResult.WON) {
                current++;
                best = Math.max(best, current);
            } else {
                current = 0;
            }
        }
        return best;
    }

    private Optional<BestHand> bestHand(User user, List<MatchPlayer> hands) {
        BestHand best = null;
        for (MatchPlayer hand : hands) {
            if (hand.getResult() != PlayerResult.WON) {
                continue;
            }
            BestHand candidate = bestHandFromState(user, hand);
            if (candidate.amountWon() > 0 && (best == null || candidate.amountWon() > best.amountWon())) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private BestHand bestHandFromState(User user, MatchPlayer hand) {
        try {
            JsonNode root = objectMapper.readTree(hand.getMatch().getServerStateJson());
            long pot = root.path("pot").asLong(0);
            for (JsonNode player : root.path("players")) {
                if (user.getId().toString().equals(player.path("userId").asText())) {
                    List<String> cards = new ArrayList<>();
                    player.path("cards").forEach(card -> cards.add(card.asText()));
                    return new BestHand(pot, handRankLabel(cards), cards);
                }
            }
        } catch (Exception ignored) {
            return BestHand.empty();
        }
        return BestHand.empty();
    }

    private String handRankLabel(List<String> cards) {
        if (cards.size() != 3) {
            return "Winning hand";
        }
        List<Card> parsed = cards.stream()
                .map(this::parseCard)
                .sorted(Comparator.comparingInt(Card::rankValue).reversed())
                .toList();
        boolean sameSuit = parsed.stream().map(Card::suit).distinct().count() == 1;
        boolean trail = parsed.stream().map(Card::rankValue).distinct().count() == 1;
        boolean sequence = sequence(parsed);
        if (trail) return "Trail";
        if (sameSuit && sequence) return "Pure sequence";
        if (sequence) return "Sequence";
        if (sameSuit) return "Colour";
        long distinct = parsed.stream().map(Card::rankValue).distinct().count();
        if (distinct == 2) return "Pair";
        return "High card";
    }

    private boolean sequence(List<Card> cards) {
        List<Integer> values = cards.stream().map(Card::rankValue).sorted(Comparator.reverseOrder()).toList();
        return values.equals(List.of(14, 13, 12))
                || values.equals(List.of(14, 3, 2))
                || (values.get(0) == values.get(1) + 1 && values.get(1) == values.get(2) + 1);
    }

    private Card parseCard(String value) {
        String rank = value.substring(0, value.length() - 1);
        String suit = value.substring(value.length() - 1);
        return new Card(rankValue(rank), suit);
    }

    private int rankValue(String rank) {
        return switch (rank.toUpperCase(Locale.ROOT)) {
            case "A" -> 14;
            case "K" -> 13;
            case "Q" -> 12;
            case "J" -> 11;
            default -> Integer.parseInt(rank);
        };
    }

    private double percent(long wins, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((wins * 1000.0 / total)) / 10.0;
    }

    private String formatNumber(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String formatSignedPercent(double value) {
        return (value >= 0 ? "↑ " : "↓ ") + String.format(Locale.US, "%.1f%%", Math.abs(value));
    }

    private record Card(int rankValue, String suit) {
    }

    private record BestHand(long amountWon, String rankLabel, List<String> cards) {
        static BestHand empty() {
            return new BestHand(0, "No winning hand yet", List.of());
        }
    }
}
