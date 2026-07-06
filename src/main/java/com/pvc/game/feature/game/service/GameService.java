package com.pvc.game.feature.game.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.account.service.PlatformAccountService;
import com.pvc.game.feature.game.dto.GameCatalogItem;
import com.pvc.game.feature.game.dto.MatchResponse;
import com.pvc.game.feature.game.engine.TeenPattiEngine;
import com.pvc.game.feature.game.entity.GameType;
import com.pvc.game.feature.game.entity.Match;
import com.pvc.game.feature.game.entity.MatchMode;
import com.pvc.game.feature.game.entity.MatchPlayer;
import com.pvc.game.feature.game.entity.MatchStatus;
import com.pvc.game.feature.game.entity.PlayerResult;
import com.pvc.game.feature.game.repository.MatchPlayerRepository;
import com.pvc.game.feature.game.repository.MatchRepository;
import com.pvc.game.feature.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final UUID PRACTICE_AI_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final TeenPattiEngine teenPattiEngine;
    private final WalletService walletService;
    private final PlatformAccountService platformAccountService;
    private final SimpMessagingTemplate messagingTemplate;

    public List<GameCatalogItem> catalog() {
        return List.of(
                new GameCatalogItem(GameType.TEEN_PATTI, "Teen Patti", true, 2, 5),
                new GameCatalogItem(GameType.CALL_BREAK, "Call Break", false, 4, 4),
                new GameCatalogItem(GameType.HEARTS, "Hearts", false, 4, 4),
                new GameCatalogItem(GameType.SPADES, "Spades", false, 4, 4),
                new GameCatalogItem(GameType.CRAZY_EIGHTS, "Crazy Eights", false, 2, 6));
    }

    @Transactional
    public MatchResponse createPractice(User user, GameType gameType, long stake) {
        ensureSupported(gameType);
        debitStake(user, stake, "PRACTICE_STAKE");
        Match match = createMatch(gameType, MatchMode.PRACTICE_AI, null, stake, 2, 2);
        match.setStatus(MatchStatus.ACTIVE);
        match.setStartedAt(Instant.now());
        matchRepository.save(match);
        addPlayer(match, user, 0, stake);
        match.setServerStateJson(teenPattiEngine.newServerState(List.of(
                new TeenPattiEngine.PlayerSeat(0, user.getId(), user.getNickname()),
                new TeenPattiEngine.PlayerSeat(1, PRACTICE_AI_USER_ID, "AI Player")),
                stake));
        matchRepository.save(match);
        publish(match);
        return response(match, user);
    }

    @Transactional
    public MatchResponse createPrivateRoom(User user, GameType gameType, long stake) {
        ensureSupported(gameType);
        debitStake(user, stake, "PRIVATE_ROOM_STAKE");
        Match match = createMatch(gameType, MatchMode.PRIVATE_ROOM, roomCode(), stake, 2, 5);
        matchRepository.save(match);
        addPlayer(match, user, 0, stake);
        publish(match);
        return response(match, user);
    }

    @Transactional
    public MatchResponse joinPrivateRoom(User user, String roomCode) {
        Match match = matchRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return joinWaitingMatch(user, match, "PRIVATE_ROOM_STAKE");
    }

    @Transactional
    public MatchResponse publicMatchmaking(User user, GameType gameType, long stake) {
        ensureSupported(gameType);
        Match match = findJoinablePublicMatch(gameType, stake)
                .orElseGet(() -> matchRepository.save(createMatch(gameType, MatchMode.PUBLIC_MATCHMAKING, null, stake, 2, 5)));
        return joinWaitingMatch(user, match, "PUBLIC_MATCH_STAKE");
    }

    @Transactional
    public MatchResponse applyMove(User user, UUID matchId, String action, long chips) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        List<MatchPlayer> players = matchPlayerRepository.findByMatchOrderBySeatNoAsc(match);
        MatchPlayer player = players.stream()
                .filter(matchPlayer -> matchPlayer.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User is not in this match"));
        if (player.getResult() == PlayerResult.WAITING_NEXT_HAND) {
            throw new IllegalStateException("Player is waiting for the next hand");
        }
        if (player.getResult() != PlayerResult.PLAYING) {
            throw new IllegalStateException("Player is not active in this hand");
        }
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException("Match is not active");
        }
        if (!matchPlayerRepository.existsByMatchAndUser(match, user)) {
            throw new IllegalStateException("User is not in this match");
        }

        if (isDealerTipAction(action)) {
            return applyDealerTip(user, match, player, chips);
        }

        TeenPattiEngine.MoveResult result = teenPattiEngine.applyMove(
                match.getServerStateJson(),
                player.getSeatNo(),
                action,
                chips);

        if (result.chipsToDebit() > 0) {
            walletService.debit(user, result.chipsToDebit(), "MATCH_MOVE", match.getId().toString());
            player.setChipsCommitted(player.getChipsCommitted() + result.chipsToDebit());
            matchPlayerRepository.save(player);
        }

        match.setServerStateJson(result.serverStateJson());
        if (!result.finished() && match.getMode() == MatchMode.PRACTICE_AI) {
            result = teenPattiEngine.applyAutomaticMove(result.serverStateJson(), 1);
            match.setServerStateJson(result.serverStateJson());
        }
        if (result.finished()) {
            finishMatch(match, players, result.winnerSeat(), result.pot());
        }
        matchRepository.save(match);
        publish(match);
        return response(match, user);
    }

    private MatchResponse applyDealerTip(User user, Match match, MatchPlayer player, long chips) {
        TeenPattiEngine.TipResult result = teenPattiEngine.applyDealerTip(
                match.getServerStateJson(),
                player.getSeatNo(),
                chips);
        walletService.debit(user, result.chipsToDebit(), "DEALER_TIP", match.getId().toString());
        platformAccountService.creditHouse(result.chipsToDebit(), "DEALER_TIP", match.getId().toString(), user.getId());
        match.setServerStateJson(result.serverStateJson());
        matchRepository.save(match);
        publish(match);
        return response(match, user);
    }

    public MatchResponse getMatch(User user, UUID id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        if (!matchPlayerRepository.existsByMatchAndUser(match, user)) {
            throw new IllegalStateException("User is not in this match");
        }
        return response(match, user);
    }

    private Optional<Match> findJoinablePublicMatch(GameType gameType, long stake) {
        List<Match> waitingMatches = matchRepository.findJoinableByStatus(
                gameType,
                MatchMode.PUBLIC_MATCHMAKING,
                MatchStatus.WAITING,
                stake,
                PlayerResult.LEFT);
        if (!waitingMatches.isEmpty()) {
            return Optional.of(waitingMatches.get(0));
        }
        List<Match> activeMatches = matchRepository.findJoinableByStatus(
                gameType,
                MatchMode.PUBLIC_MATCHMAKING,
                MatchStatus.ACTIVE,
                stake,
                PlayerResult.LEFT);
        if (!activeMatches.isEmpty()) {
            return Optional.of(activeMatches.get(0));
        }
        return Optional.empty();
    }

    private MatchResponse joinWaitingMatch(User user, Match match, String stakeSource) {
        Optional<MatchPlayer> existingPlayer = matchPlayerRepository.findByMatchAndUser(match, user);
        if (existingPlayer.isPresent() && existingPlayer.get().getResult() != PlayerResult.LEFT) {
            return response(match, user);
        }
        if (match.getStatus() != MatchStatus.WAITING && match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException("Room is not joinable");
        }
        long seatedPlayerCount = matchPlayerRepository.countByMatchAndResultNot(match, PlayerResult.LEFT);
        if (seatedPlayerCount >= match.getMaxPlayers()) {
            throw new IllegalStateException("Match is full");
        }
        debitStake(user, match.getStake(), stakeSource);
        PlayerResult result = match.getStatus() == MatchStatus.ACTIVE
                ? PlayerResult.WAITING_NEXT_HAND
                : PlayerResult.PLAYING;
        addOrReusePlayer(match, user, firstAvailableSeat(match), match.getStake(), result);
        if (match.getStatus() == MatchStatus.WAITING
                && seatedPlayerCount + 1 >= match.getMinPlayers()) {
            match.setStatus(MatchStatus.ACTIVE);
            match.setStartedAt(Instant.now());
            List<MatchPlayer> players = activePlayers(match);
            match.setServerStateJson(teenPattiEngine.newServerState(players.stream()
                    .map(player -> new TeenPattiEngine.PlayerSeat(
                            player.getSeatNo(),
                            player.getUser().getId(),
                            player.getUser().getNickname()))
                    .toList(), match.getStake()));
            matchRepository.save(match);
        }
        publish(match);
        return response(match, user);
    }

    private Match createMatch(GameType gameType, MatchMode mode, String roomCode, long stake, int minPlayers, int maxPlayers) {
        Match match = new Match();
        match.setGameType(gameType);
        match.setMode(mode);
        match.setRoomCode(roomCode);
        match.setStake(stake);
        match.setMinPlayers(minPlayers);
        match.setMaxPlayers(maxPlayers);
        match.setServerStateJson("{\"phase\":\"WAITING\"}");
        return match;
    }

    private MatchPlayer addPlayer(Match match, User user, int seatNo, long stake) {
        return addOrReusePlayer(match, user, seatNo, stake, PlayerResult.PLAYING);
    }

    private MatchPlayer addOrReusePlayer(Match match, User user, int seatNo, long stake, PlayerResult result) {
        MatchPlayer player = matchPlayerRepository.findByMatchAndUser(match, user).orElseGet(MatchPlayer::new);
        player.setMatch(match);
        player.setUser(user);
        player.setSeatNo(seatNo);
        player.setChipsCommitted(stake);
        player.setResult(result);
        return matchPlayerRepository.save(player);
    }

    private int firstAvailableSeat(Match match) {
        Set<Integer> occupiedSeats = new HashSet<>();
        matchPlayerRepository.findByMatchAndResultNotOrderBySeatNoAsc(match, PlayerResult.LEFT)
                .forEach(player -> occupiedSeats.add(player.getSeatNo()));
        for (int seatNo = 0; seatNo < match.getMaxPlayers(); seatNo++) {
            if (!occupiedSeats.contains(seatNo)) {
                return seatNo;
            }
        }
        throw new IllegalStateException("Match is full");
    }

    private List<MatchPlayer> activePlayers(Match match) {
        return matchPlayerRepository.findByMatchOrderBySeatNoAsc(match).stream()
                .filter(player -> player.getResult() == PlayerResult.PLAYING)
                .toList();
    }

    private MatchResponse response(Match match, User viewer) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatchOrderBySeatNoAsc(match);
        Integer viewerSeat = players.stream()
                .filter(player -> player.getUser().getId().equals(viewer.getId()))
                .map(MatchPlayer::getSeatNo)
                .findFirst()
                .orElse(null);
        return MatchResponse.from(match, players, teenPattiEngine.publicState(match.getServerStateJson(), viewerSeat));
    }

    private void publish(Match match) {
        List<MatchPlayer> players = matchPlayerRepository.findByMatchOrderBySeatNoAsc(match);
        MatchResponse response = MatchResponse.from(match, players, teenPattiEngine.publicState(match.getServerStateJson(), null));
        messagingTemplate.convertAndSend("/topic/matches/" + response.getId(), response);
    }

    private void finishMatch(Match match, List<MatchPlayer> players, int winnerSeat, long pot) {
        match.setStatus(MatchStatus.FINISHED);
        match.setFinishedAt(Instant.now());

        MatchPlayer winner = players.stream()
                .filter(player -> player.getResult() == PlayerResult.PLAYING)
                .filter(player -> player.getSeatNo() == winnerSeat)
                .findFirst()
                .orElse(null);

        for (MatchPlayer player : players) {
            if (player.getResult() == PlayerResult.PLAYING) {
                player.setResult(player.getSeatNo() == winnerSeat ? PlayerResult.WON : PlayerResult.LOST);
            }
        }
        matchPlayerRepository.saveAll(players);

        if (winner != null) {
            match.setWinnerUserId(winner.getUser().getId());
            if (pot > 0) {
                walletService.credit(winner.getUser(), pot, "MATCH_WIN", match.getId().toString());
            }
        }
        publish(match);
        startNextHandIfReady(match, players);
    }

    private void startNextHandIfReady(Match match, List<MatchPlayer> players) {
        if (match.getMode() == MatchMode.PRACTICE_AI) {
            return;
        }

        for (MatchPlayer player : players) {
            if (player.getResult() == PlayerResult.WON || player.getResult() == PlayerResult.LOST) {
                if (match.getStake() > 0 && walletService.getWallet(player.getUser()).getBalance() < match.getStake()) {
                    player.setResult(PlayerResult.LEFT);
                    player.setChipsCommitted(0);
                    continue;
                }
                debitStake(player.getUser(), match.getStake(), "NEXT_HAND_STAKE");
            }
            if (player.getResult() != PlayerResult.LEFT) {
                player.setResult(PlayerResult.PLAYING);
                player.setChipsCommitted(match.getStake());
            }
        }

        List<MatchPlayer> nextHandPlayers = players.stream()
                .filter(player -> player.getResult() == PlayerResult.PLAYING)
                .sorted(Comparator.comparingInt(MatchPlayer::getSeatNo))
                .toList();
        matchPlayerRepository.saveAll(players);

        match.setWinnerUserId(null);
        if (nextHandPlayers.size() >= match.getMinPlayers()) {
            match.setStatus(MatchStatus.ACTIVE);
            match.setStartedAt(Instant.now());
            match.setFinishedAt(null);
            match.setServerStateJson(teenPattiEngine.newServerState(nextHandPlayers.stream()
                    .map(player -> new TeenPattiEngine.PlayerSeat(
                            player.getSeatNo(),
                            player.getUser().getId(),
                            player.getUser().getNickname()))
                    .toList(), match.getStake()));
        } else {
            match.setStatus(MatchStatus.WAITING);
            match.setStartedAt(null);
            match.setServerStateJson("{\"phase\":\"WAITING\"}");
        }
    }

    private void debitStake(User user, long stake, String source) {
        if (stake > 0) {
            walletService.debit(user, stake, source, null);
        }
    }

    private void ensureSupported(GameType gameType) {
        if (gameType != GameType.TEEN_PATTI) {
            throw new IllegalArgumentException("Only Teen Patti is enabled in version 1");
        }
    }

    private boolean isDealerTipAction(String action) {
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT).replace("-", "_");
        return normalizedAction.equals("DEALER_TIP") || normalizedAction.equals("TIP_DEALER");
    }

    private String roomCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
