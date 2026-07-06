package com.pvc.game.feature.leaderboard.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.auth.repository.UserRepository;
import com.pvc.game.feature.leaderboard.dto.LeaderboardRow;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<LeaderboardRow>> leaderboard(@RequestParam(defaultValue = "ALL_TIME") String period) {
        return ApiResponse.ok(userRepository.findAll().stream()
                .sorted((left, right) -> Long.compare(right.getXp(), left.getXp()))
                .limit(100)
                .map(user -> new LeaderboardRow(user.getId(), user.getNickname(), user.getXp(), user.getLevel()))
                .toList());
    }
}
