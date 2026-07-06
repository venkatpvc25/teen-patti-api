package com.pvc.game.feature.game.dto;

import java.util.List;
import java.util.UUID;

import com.pvc.game.feature.game.entity.Match;
import com.pvc.game.feature.game.entity.MatchPlayer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchResponse {
    private UUID id;
    private String gameType;
    private String mode;
    private String status;
    private String roomCode;
    private long stake;
    private String serverStateJson;
    private List<PlayerLine> players;

    public static MatchResponse from(Match match, List<MatchPlayer> players, String publicStateJson) {
        return new MatchResponse(
                match.getId(),
                match.getGameType().name(),
                match.getMode().name(),
                match.getStatus().name(),
                match.getRoomCode(),
                match.getStake(),
                publicStateJson,
                players.stream().map(PlayerLine::from).toList());
    }

    @Data
    @AllArgsConstructor
    public static class PlayerLine {
        private UUID userId;
        private String nickname;
        private int seatNo;
        private long chipsCommitted;
        private String result;

        public static PlayerLine from(MatchPlayer player) {
            return new PlayerLine(
                    player.getUser().getId(),
                    player.getUser().getNickname(),
                    player.getSeatNo(),
                    player.getChipsCommitted(),
                    player.getResult().name());
        }
    }
}
