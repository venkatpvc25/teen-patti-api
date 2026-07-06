package com.pvc.game.feature.economy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.economy.dto.RewardResponse;
import com.pvc.game.feature.economy.entity.Mission;
import com.pvc.game.feature.economy.service.EconomyService;
import com.pvc.game.feature.user.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class EconomyController {

    private final EconomyService economyService;
    private final CurrentUserService currentUserService;

    @PostMapping("/daily-rewards/claim")
    public ApiResponse<RewardResponse> claimDaily() {
        return ApiResponse.ok(economyService.claimDaily(currentUserService.requireCurrentUser()));
    }

    @PostMapping("/spin-wheel/spin")
    public ApiResponse<RewardResponse> spin() {
        return ApiResponse.ok(economyService.spin(currentUserService.requireCurrentUser()));
    }

    @GetMapping("/missions")
    public ApiResponse<List<Mission>> missions() {
        return ApiResponse.ok(economyService.missions());
    }
}
