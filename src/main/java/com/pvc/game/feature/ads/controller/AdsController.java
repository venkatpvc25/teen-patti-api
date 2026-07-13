package com.pvc.game.feature.ads.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.ads.dto.AdRewardRequest;
import com.pvc.game.feature.ads.entity.AdvertisementReward;
import com.pvc.game.feature.ads.repository.AdvertisementRewardRepository;
import com.pvc.game.feature.economy.dto.RewardResponse;
import com.pvc.game.feature.user.service.CurrentUserService;
import com.pvc.game.feature.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
@Slf4j
public class AdsController {

    private static final long REWARDED_AD_CHIPS = 1_000;

    private final CurrentUserService currentUserService;
    private final AdvertisementRewardRepository rewardRepository;
    private final WalletService walletService;

    @PostMapping("/rewarded/claim")
    public ApiResponse<RewardResponse> claimRewardedAd(@Valid @RequestBody AdRewardRequest request) {
        var user = currentUserService.requireCurrentUser();
        log.info("Rewarded ad claim requested userId={} placement={}", user.getId(), request.getPlacement());
        AdvertisementReward reward = new AdvertisementReward();
        reward.setUser(user);
        reward.setPlacement(request.getPlacement());
        reward.setChips(REWARDED_AD_CHIPS);
        rewardRepository.save(reward);
        var wallet = walletService.credit(user, REWARDED_AD_CHIPS, "REWARDED_AD", reward.getId().toString());
        log.info("Rewarded ad claim completed userId={} rewardId={} chips={} balance={}",
                user.getId(), reward.getId(), REWARDED_AD_CHIPS, wallet.getBalance());
        return ApiResponse.ok(new RewardResponse(REWARDED_AD_CHIPS, wallet.getBalance(), "REWARDED_AD"));
    }
}
