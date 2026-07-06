package com.pvc.game.feature.game.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeenPattiEngine {

    private static final List<String> SUPPORTED_ACTIONS = List.of("SEE", "PACK", "BLIND", "CHAAL", "SHOW", "SIDE_SHOW");

    private final ObjectMapper objectMapper;

    public String newServerState(List<PlayerSeat> seats, long bootAmount) {
        if (seats.size() < 2 || seats.size() > 5) {
            throw new IllegalArgumentException("Teen Patti requires 2 to 5 players");
        }

        List<String> deck = CardDeck.shuffled();
        TeenPattiState state = new TeenPattiState();
        state.phase = "DEALT";
        state.bootAmount = Math.max(0, bootAmount);
        state.currentStake = Math.max(1, bootAmount);
        state.pot = Math.max(0, bootAmount) * seats.size();
        state.turnSeat = seats.get(0).seatNo();
        state.dealerSeat = seats.get(0).seatNo();
        state.deckRemaining = deck.size() - (seats.size() * 3);

        int cursor = 0;
        for (PlayerSeat seat : seats) {
            PlayerState player = new PlayerState();
            player.seatNo = seat.seatNo();
            player.userId = seat.userId().toString();
            player.nickname = seat.nickname();
            player.cards = List.of(deck.get(cursor++), deck.get(cursor++), deck.get(cursor++));
            player.blind = true;
            player.active = true;
            player.packed = false;
            player.chipsCommitted = Math.max(0, bootAmount);
            state.players.add(player);
        }
        return write(state);
    }

    public MoveResult applyMove(String serverStateJson, int seatNo, String action, long chips) {
        TeenPattiState state = read(serverStateJson);
        ensureActiveState(state);
        String normalizedAction = normalizeAction(action);
        if (!isSupportedAction(normalizedAction)) {
            throw new IllegalArgumentException("Unsupported move");
        }

        PlayerState player = findPlayer(state, seatNo);
        if (!player.active || player.packed) {
            throw new IllegalStateException("Player is no longer active");
        }
        if (state.turnSeat != seatNo) {
            throw new IllegalStateException("It is not this player's turn");
        }

        long debit = switch (normalizedAction) {
            case "SEE" -> see(state, player);
            case "PACK" -> pack(state, player);
            case "BLIND" -> blind(state, player, chips);
            case "CHAAL" -> chaal(state, player, chips);
            case "SHOW" -> show(state, player, chips);
            case "SIDE_SHOW" -> sideShow(state, player, chips);
            default -> throw new IllegalArgumentException("Unsupported move");
        };

        player.chipsCommitted += debit;
        state.pot += debit;
        state.lastAction = normalizedAction;
        state.lastActionSeat = seatNo;

        if (!"FINISHED".equals(state.phase)) {
            finishIfOnlyOneActive(state);
        }
        if (!"FINISHED".equals(state.phase)) {
            state.turnSeat = nextActiveSeat(state, seatNo);
        }

        return result(state, debit);
    }

    public TipResult applyDealerTip(String serverStateJson, int seatNo, long chips) {
        TeenPattiState state = read(serverStateJson);
        ensureActiveState(state);
        if (chips <= 0) {
            throw new IllegalArgumentException("Dealer tip must be greater than 0 chips");
        }

        PlayerState player = findPlayer(state, seatNo);
        if (!player.active || player.packed) {
            throw new IllegalStateException("Player is no longer active");
        }

        player.dealerTips += chips;
        state.dealerTipTotal += chips;
        state.lastAction = "DEALER_TIP";
        state.lastActionSeat = seatNo;
        state.lastDealerTipSeat = seatNo;
        state.lastDealerTipAmount = chips;
        return new TipResult(write(state), publicState(serverStateJsonFor(state), null), chips);
    }

    public MoveResult applyAutomaticMove(String serverStateJson, int seatNo) {
        TeenPattiState state = read(serverStateJson);
        ensureActiveState(state);
        PlayerState player = findPlayer(state, seatNo);
        if (state.turnSeat != seatNo || !player.active || player.packed) {
            return result(state, 0);
        }

        long debit;
        List<PlayerState> active = activePlayers(state);
        if (active.size() == 2) {
            long required = player.blind ? state.currentStake : state.currentStake * 2;
            debit = show(state, player, required);
            state.lastAction = "SHOW";
        } else if (player.blind) {
            debit = blind(state, player, state.currentStake);
            state.lastAction = "BLIND";
        } else {
            debit = chaal(state, player, state.currentStake * 2);
            state.lastAction = "CHAAL";
        }

        player.chipsCommitted += debit;
        state.pot += debit;
        state.lastActionSeat = seatNo;
        if (!"FINISHED".equals(state.phase)) {
            finishIfOnlyOneActive(state);
        }
        if (!"FINISHED".equals(state.phase)) {
            state.turnSeat = nextActiveSeat(state, seatNo);
        }
        return result(state, debit);
    }

    public String publicState(String serverStateJson, Integer viewerSeatNo) {
        TeenPattiState state = read(serverStateJson);
        PublicState view = new PublicState();
        view.phase = state.phase;
        view.bootAmount = state.bootAmount;
        view.currentStake = state.currentStake;
        view.pot = state.pot;
        view.turnSeat = state.turnSeat;
        view.dealerSeat = state.dealerSeat;
        view.dealerTipTotal = state.dealerTipTotal;
        view.deckRemaining = state.deckRemaining;
        view.lastAction = state.lastAction;
        view.lastActionSeat = state.lastActionSeat;
        view.lastDealerTipSeat = state.lastDealerTipSeat;
        view.lastDealerTipAmount = state.lastDealerTipAmount;
        view.winnerSeat = state.winnerSeat;
        view.winnerUserId = state.winnerUserId;

        for (PlayerState player : state.players) {
            PublicPlayer publicPlayer = new PublicPlayer();
            publicPlayer.seatNo = player.seatNo;
            publicPlayer.userId = player.userId;
            publicPlayer.nickname = player.nickname;
            publicPlayer.blind = player.blind;
            publicPlayer.active = player.active;
            publicPlayer.packed = player.packed;
            publicPlayer.chipsCommitted = player.chipsCommitted;
            publicPlayer.dealerTips = player.dealerTips;
            publicPlayer.cardsVisible = viewerSeatNo != null && viewerSeatNo == player.seatNo || "FINISHED".equals(state.phase);
            if (publicPlayer.cardsVisible) {
                publicPlayer.cards = player.cards;
                publicPlayer.handRank = rank(player.cards).label;
            }
            view.players.add(publicPlayer);
        }
        return write(view);
    }

    public boolean isSupportedAction(String action) {
        return SUPPORTED_ACTIONS.contains(normalizeAction(action));
    }

    private MoveResult result(TeenPattiState state, long debit) {
        return new MoveResult(write(state), publicState(serverStateJsonFor(state), null), debit, state.pot, state.winnerSeat, "FINISHED".equals(state.phase));
    }

    private long see(TeenPattiState state, PlayerState player) {
        player.blind = false;
        return 0;
    }

    private long pack(TeenPattiState state, PlayerState player) {
        player.active = false;
        player.packed = true;
        return 0;
    }

    private long blind(TeenPattiState state, PlayerState player, long chips) {
        if (!player.blind) {
            throw new IllegalStateException("Seen players must use CHAAL");
        }
        long min = state.currentStake;
        long max = Math.max(min, state.currentStake * 2);
        requireBet(chips, min, max, "Blind bet");
        state.currentStake = Math.max(state.currentStake, chips);
        return chips;
    }

    private long chaal(TeenPattiState state, PlayerState player, long chips) {
        if (player.blind) {
            throw new IllegalStateException("Blind players must SEE before CHAAL");
        }
        long min = state.currentStake * 2;
        long max = Math.max(min, state.currentStake * 4);
        requireBet(chips, min, max, "Chaal bet");
        state.currentStake = Math.max(state.currentStake, chips / 2);
        return chips;
    }

    private long show(TeenPattiState state, PlayerState player, long chips) {
        List<PlayerState> active = activePlayers(state);
        if (active.size() != 2) {
            throw new IllegalStateException("SHOW is allowed only when two players remain");
        }
        long required = player.blind ? state.currentStake : state.currentStake * 2;
        requireExactBet(chips, required, "Show");
        PlayerState opponent = active.stream()
                .filter(activePlayer -> activePlayer.seatNo != player.seatNo)
                .findFirst()
                .orElseThrow();
        PlayerState winner = compareHands(player, opponent) >= 0 ? player : opponent;
        finish(state, winner);
        return chips;
    }

    private long sideShow(TeenPattiState state, PlayerState player, long chips) {
        if (player.blind) {
            throw new IllegalStateException("Blind players must SEE before SIDE_SHOW");
        }
        List<PlayerState> active = activePlayers(state);
        if (active.size() <= 2) {
            throw new IllegalStateException("Use SHOW when only two players remain");
        }
        long required = state.currentStake * 2;
        requireExactBet(chips, required, "Side show");

        PlayerState opponent = previousActiveSeenPlayer(state, player.seatNo);
        PlayerState loser = compareHands(player, opponent) >= 0 ? opponent : player;
        loser.active = false;
        loser.packed = true;
        return chips;
    }

    private PlayerState previousActiveSeenPlayer(TeenPattiState state, int seatNo) {
        List<PlayerState> ordered = state.players.stream()
                .sorted(Comparator.comparingInt(player -> player.seatNo))
                .toList();
        int index = 0;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).seatNo == seatNo) {
                index = i;
                break;
            }
        }
        for (int offset = 1; offset < ordered.size(); offset++) {
            PlayerState candidate = ordered.get((index - offset + ordered.size()) % ordered.size());
            if (candidate.active && !candidate.packed && !candidate.blind) {
                return candidate;
            }
        }
        throw new IllegalStateException("No seen opponent is available for SIDE_SHOW");
    }

    private void finishIfOnlyOneActive(TeenPattiState state) {
        List<PlayerState> active = activePlayers(state);
        if (active.size() == 1) {
            finish(state, active.get(0));
        }
    }

    private void finish(TeenPattiState state, PlayerState winner) {
        state.phase = "FINISHED";
        state.turnSeat = -1;
        state.winnerSeat = winner.seatNo;
        state.winnerUserId = winner.userId;
        for (PlayerState player : state.players) {
            player.active = false;
        }
    }

    private int nextActiveSeat(TeenPattiState state, int currentSeatNo) {
        List<PlayerState> ordered = state.players.stream()
                .sorted(Comparator.comparingInt(player -> player.seatNo))
                .toList();
        int index = 0;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).seatNo == currentSeatNo) {
                index = i;
                break;
            }
        }
        for (int offset = 1; offset <= ordered.size(); offset++) {
            PlayerState candidate = ordered.get((index + offset) % ordered.size());
            if (candidate.active && !candidate.packed) {
                return candidate.seatNo;
            }
        }
        return -1;
    }

    private PlayerState findPlayer(TeenPattiState state, int seatNo) {
        return state.players.stream()
                .filter(player -> player.seatNo == seatNo)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player is not seated in this match"));
    }

    private List<PlayerState> activePlayers(TeenPattiState state) {
        return state.players.stream()
                .filter(player -> player.active && !player.packed)
                .toList();
    }

    private void ensureActiveState(TeenPattiState state) {
        if ("FINISHED".equals(state.phase)) {
            throw new IllegalStateException("Match is already finished");
        }
    }

    private void requireBet(long chips, long min, long max, String label) {
        if (chips < min || chips > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max + " chips");
        }
    }

    private void requireExactBet(long chips, long required, String label) {
        if (chips != required) {
            throw new IllegalArgumentException(label + " requires exactly " + required + " chips");
        }
    }

    private int compareHands(PlayerState left, PlayerState right) {
        HandRank leftRank = rank(left.cards);
        HandRank rightRank = rank(right.cards);
        int categoryCompare = Integer.compare(leftRank.category, rightRank.category);
        if (categoryCompare != 0) {
            return categoryCompare;
        }
        for (int i = 0; i < Math.min(leftRank.values.size(), rightRank.values.size()); i++) {
            int valueCompare = Integer.compare(leftRank.values.get(i), rightRank.values.get(i));
            if (valueCompare != 0) {
                return valueCompare;
            }
        }
        return 0;
    }

    private HandRank rank(List<String> cards) {
        List<Card> parsed = cards.stream()
                .map(this::parseCard)
                .sorted(Comparator.comparingInt(Card::rankValue).reversed())
                .toList();
        boolean sameSuit = parsed.stream().map(Card::suit).distinct().count() == 1;
        boolean trail = parsed.stream().map(Card::rankValue).distinct().count() == 1;
        Sequence sequence = sequence(parsed);

        if (trail) {
            return new HandRank(6, List.of(parsed.get(0).rankValue()), "TRAIL");
        }
        if (sameSuit && sequence.present) {
            return new HandRank(5, sequence.values, "PURE_SEQUENCE");
        }
        if (sequence.present) {
            return new HandRank(4, sequence.values, "SEQUENCE");
        }
        if (sameSuit) {
            return new HandRank(3, parsed.stream().map(Card::rankValue).toList(), "COLOR");
        }
        List<Integer> distinct = parsed.stream().map(Card::rankValue).distinct().toList();
        if (distinct.size() == 2) {
            int pair = distinct.stream()
                    .filter(value -> parsed.stream().filter(card -> card.rankValue() == value).count() == 2)
                    .findFirst()
                    .orElseThrow();
            int kicker = distinct.stream()
                    .filter(value -> value != pair)
                    .findFirst()
                    .orElseThrow();
            return new HandRank(2, List.of(pair, kicker), "PAIR");
        }
        return new HandRank(1, parsed.stream().map(Card::rankValue).toList(), "HIGH_CARD");
    }

    private Sequence sequence(List<Card> cards) {
        List<Integer> values = cards.stream().map(Card::rankValue).sorted(Comparator.reverseOrder()).toList();
        if (values.equals(List.of(14, 13, 12))) {
            return new Sequence(true, List.of(14, 13, 12));
        }
        if (values.equals(List.of(14, 3, 2))) {
            return new Sequence(true, List.of(13, 3, 2));
        }
        if (values.get(0) == values.get(1) + 1 && values.get(1) == values.get(2) + 1) {
            return new Sequence(true, values);
        }
        return new Sequence(false, values);
    }

    private Card parseCard(String value) {
        String rank = value.substring(0, value.length() - 1);
        String suit = value.substring(value.length() - 1);
        return new Card(rankValue(rank), suit);
    }

    private int rankValue(String rank) {
        return switch (rank) {
            case "A" -> 14;
            case "K" -> 13;
            case "Q" -> 12;
            case "J" -> 11;
            default -> Integer.parseInt(rank);
        };
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase(Locale.ROOT).replace("-", "_");
    }

    private TeenPattiState read(String json) {
        try {
            return objectMapper.readValue(json, TeenPattiState.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Teen Patti state", exception);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize Teen Patti state", exception);
        }
    }

    private String serverStateJsonFor(TeenPattiState state) {
        return write(state);
    }

    public record PlayerSeat(int seatNo, UUID userId, String nickname) {
    }

    public record MoveResult(String serverStateJson, String publicStateJson, long chipsToDebit, long pot, int winnerSeat, boolean finished) {
    }

    public record TipResult(String serverStateJson, String publicStateJson, long chipsToDebit) {
    }

    private record Card(int rankValue, String suit) {
    }

    private record HandRank(int category, List<Integer> values, String label) {
    }

    private record Sequence(boolean present, List<Integer> values) {
    }

    public static class TeenPattiState {
        public String phase;
        public long bootAmount;
        public long currentStake;
        public long pot;
        public int turnSeat;
        public int dealerSeat;
        public long dealerTipTotal;
        public int deckRemaining;
        public String lastAction;
        public int lastActionSeat = -1;
        public int lastDealerTipSeat = -1;
        public long lastDealerTipAmount;
        public int winnerSeat = -1;
        public String winnerUserId;
        public List<PlayerState> players = new ArrayList<>();
    }

    public static class PlayerState {
        public int seatNo;
        public String userId;
        public String nickname;
        public List<String> cards = new ArrayList<>();
        public boolean blind;
        public boolean active;
        public boolean packed;
        public long chipsCommitted;
        public long dealerTips;
    }

    public static class PublicState {
        public String phase;
        public long bootAmount;
        public long currentStake;
        public long pot;
        public int turnSeat;
        public int dealerSeat;
        public long dealerTipTotal;
        public int deckRemaining;
        public String lastAction;
        public int lastActionSeat;
        public int lastDealerTipSeat;
        public long lastDealerTipAmount;
        public int winnerSeat;
        public String winnerUserId;
        public List<PublicPlayer> players = new ArrayList<>();
    }

    public static class PublicPlayer {
        public int seatNo;
        public String userId;
        public String nickname;
        public boolean blind;
        public boolean active;
        public boolean packed;
        public long chipsCommitted;
        public long dealerTips;
        public boolean cardsVisible;
        public List<String> cards;
        public String handRank;
    }
}
