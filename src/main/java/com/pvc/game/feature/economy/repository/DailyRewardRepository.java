package com.pvc.game.feature.economy.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.economy.entity.DailyReward;

public interface DailyRewardRepository extends JpaRepository<DailyReward, UUID> {
    Optional<DailyReward> findByUserAndRewardDate(User user, LocalDate rewardDate);
}
