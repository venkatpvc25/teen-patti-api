package com.pvc.game.feature.economy.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.economy.dto.RewardResponse;
import com.pvc.game.feature.economy.entity.DailyReward;
import com.pvc.game.feature.economy.entity.Mission;
import com.pvc.game.feature.economy.repository.DailyRewardRepository;
import com.pvc.game.feature.economy.repository.MissionRepository;
import com.pvc.game.feature.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EconomyService {

    private static final long DAILY_REWARD = 1_000;

    private final DailyRewardRepository dailyRewardRepository;
    private final MissionRepository missionRepository;
    private final WalletService walletService;

    @Transactional
    public RewardResponse claimDaily(User user) {
        LocalDate today = LocalDate.now();
        log.info("Daily reward claim requested userId={} rewardDate={}", user.getId(), today);
        dailyRewardRepository.findByUserAndRewardDate(user, today)
                .ifPresent(reward -> {
                    log.warn("Daily reward claim rejected alreadyClaimed userId={} rewardId={} rewardDate={}",
                            user.getId(), reward.getId(), today);
                    throw new IllegalStateException("Daily reward already claimed");
                });
        DailyReward reward = new DailyReward();
        reward.setUser(user);
        reward.setRewardDate(today);
        reward.setChips(DAILY_REWARD);
        dailyRewardRepository.save(reward);
        var wallet = walletService.credit(user, DAILY_REWARD, "DAILY_REWARD", reward.getId().toString());
        log.info("Daily reward claim completed userId={} rewardId={} chips={} balance={}",
                user.getId(), reward.getId(), DAILY_REWARD, wallet.getBalance());
        return new RewardResponse(DAILY_REWARD, wallet.getBalance(), "DAILY_REWARD");
    }

    @Transactional
    public RewardResponse spin(User user) {
        long chips = List.of(100L, 250L, 500L, 1_000L, 2_500L)
                .get(ThreadLocalRandom.current().nextInt(5));
        var wallet = walletService.credit(user, chips, "SPIN_WHEEL", null);
        log.info("Spin wheel completed userId={} chips={} balance={}", user.getId(), chips, wallet.getBalance());
        return new RewardResponse(chips, wallet.getBalance(), "SPIN_WHEEL");
    }

    public List<Mission> missions() {
        List<Mission> missions = missionRepository.findByActiveTrue();
        log.info("Missions fetched count={}", missions.size());
        return missions;
    }
}
