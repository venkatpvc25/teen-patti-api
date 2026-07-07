package com.pvc.game.feature.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.dashboard.dto.DashboardResponse;
import com.pvc.game.feature.dashboard.service.DashboardService;
import com.pvc.game.feature.user.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final CurrentUserService currentUserService;
    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(dashboardService.dashboardFor(currentUserService.requireCurrentUser()));
    }
}
