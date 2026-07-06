package com.pvc.game.feature.game.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
