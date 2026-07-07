package com.pvc.game.feature.game.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.game.feature.game.entity.GameType;
import com.pvc.game.feature.game.entity.Match;
import com.pvc.game.feature.game.entity.MatchMode;
import com.pvc.game.feature.game.entity.PlayerResult;
import com.pvc.game.feature.game.entity.MatchStatus;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByStatus(MatchStatus status);

    Optional<Match> findFirstByGameTypeAndModeAndStatusAndStakeOrderByCreatedAtAsc(
            GameType gameType,
            MatchMode mode,
            MatchStatus status,
            long stake);

    Optional<Match> findByRoomCode(String roomCode);

    @Query("""
            select m from Match m
            where m.gameType = :gameType
              and m.mode = :mode
              and m.status = :status
              and m.stake = :stake
              and (select count(mp) from MatchPlayer mp where mp.match = m and mp.result <> :excludedResult) < m.maxPlayers
            order by m.createdAt asc
            """)
    List<Match> findJoinableByStatus(
            @Param("gameType") GameType gameType,
            @Param("mode") MatchMode mode,
            @Param("status") MatchStatus status,
            @Param("stake") long stake,
            @Param("excludedResult") PlayerResult excludedResult);
}
