package com.pvc.game.feature.game.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.game.entity.Match;
import com.pvc.game.feature.game.entity.MatchPlayer;
import com.pvc.game.feature.game.entity.PlayerResult;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {
    List<MatchPlayer> findByMatchOrderBySeatNoAsc(Match match);
    List<MatchPlayer> findByMatchAndResultNotOrderBySeatNoAsc(Match match, PlayerResult result);
    long countByMatch(Match match);
    long countByMatchAndResultNot(Match match, PlayerResult result);
    boolean existsByMatchAndUser(Match match, User user);
    Optional<MatchPlayer> findByMatchAndUser(Match match, User user);

    long countByUserAndResultIn(User user, List<PlayerResult> results);
    long countByUserAndResult(User user, PlayerResult result);

    @Query("""
            select count(mp)
            from MatchPlayer mp
            where mp.user = :user
              and mp.result in :results
              and mp.match.finishedAt >= :since
            """)
    long countFinishedSince(
            @Param("user") User user,
            @Param("results") List<PlayerResult> results,
            @Param("since") java.time.Instant since);

    @Query("""
            select count(mp)
            from MatchPlayer mp
            where mp.user = :user
              and mp.result = :result
              and mp.match.finishedAt >= :since
            """)
    long countByResultSince(
            @Param("user") User user,
            @Param("result") PlayerResult result,
            @Param("since") java.time.Instant since);

    @Query("""
            select mp
            from MatchPlayer mp
            join fetch mp.match m
            where mp.user = :user
              and mp.result in :results
              and m.finishedAt is not null
            order by m.finishedAt desc
            """)
    List<MatchPlayer> findFinishedByUserOrderByFinishedAtDesc(
            @Param("user") User user,
            @Param("results") List<PlayerResult> results);
}
