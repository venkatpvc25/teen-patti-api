package com.pvc.game.feature.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.auth.dto.UserSummary;
import com.pvc.game.feature.user.dto.ProfileResponse;
import com.pvc.game.feature.user.service.CurrentUserService;
import com.pvc.game.feature.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final WalletService walletService;

    @GetMapping
    public ApiResponse<ProfileResponse> me() {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.ok(new ProfileResponse(
                UserSummary.from(user),
                walletService.getWallet(user).getBalance()));
    }
}
