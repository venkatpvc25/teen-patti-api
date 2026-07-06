package com.pvc.game.feature.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.user.service.CurrentUserService;
import com.pvc.game.feature.wallet.dto.WalletResponse;
import com.pvc.game.feature.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final CurrentUserService currentUserService;
    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletResponse> wallet() {
        var user = currentUserService.requireCurrentUser();
        var wallet = walletService.getWallet(user);
        var transactions = walletService.recentTransactions(user).stream()
                .map(WalletResponse.TransactionLine::from)
                .toList();
        return ApiResponse.ok(new WalletResponse(wallet.getBalance(), transactions));
    }
}
