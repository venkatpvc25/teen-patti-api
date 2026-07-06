package com.pvc.game.feature.game.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.game.dto.CreateMatchRequest;
import com.pvc.game.feature.game.dto.GameCatalogItem;
import com.pvc.game.feature.game.dto.JoinRoomRequest;
import com.pvc.game.feature.game.dto.MatchResponse;
import com.pvc.game.feature.game.dto.MoveRequest;
import com.pvc.game.feature.game.service.GameService;
import com.pvc.game.feature.user.service.CurrentUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final CurrentUserService currentUserService;

    @GetMapping("/games")
    public ApiResponse<List<GameCatalogItem>> games() {
        return ApiResponse.ok(gameService.catalog());
    }

    @PostMapping("/games/practice")
    public ApiResponse<MatchResponse> practice(@Valid @RequestBody CreateMatchRequest request) {
        return ApiResponse.ok(gameService.createPractice(
                currentUserService.requireCurrentUser(),
                request.getGameType(),
                request.getStake()));
    }

    @PostMapping("/rooms/private")
    public ApiResponse<MatchResponse> createPrivateRoom(@Valid @RequestBody CreateMatchRequest request) {
        return ApiResponse.ok(gameService.createPrivateRoom(
                currentUserService.requireCurrentUser(),
                request.getGameType(),
                request.getStake()));
    }

    @PostMapping("/rooms/private/join")
    public ApiResponse<MatchResponse> joinPrivateRoom(@Valid @RequestBody JoinRoomRequest request) {
        return ApiResponse.ok(gameService.joinPrivateRoom(
                currentUserService.requireCurrentUser(),
                request.getRoomCode()));
    }

    @PostMapping("/matchmaking/public")
    public ApiResponse<MatchResponse> publicMatchmaking(@Valid @RequestBody CreateMatchRequest request) {
        return ApiResponse.ok(gameService.publicMatchmaking(
                currentUserService.requireCurrentUser(),
                request.getGameType(),
                request.getStake()));
    }

    @PostMapping("/matches/{id}/moves")
    public ApiResponse<MatchResponse> move(@PathVariable UUID id, @Valid @RequestBody MoveRequest request) {
        return ApiResponse.ok(gameService.applyMove(
                currentUserService.requireCurrentUser(),
                id,
                request.getAction(),
                request.getChips()));
    }

    @GetMapping("/matches/{id}")
    public ApiResponse<MatchResponse> match(@PathVariable UUID id) {
        return ApiResponse.ok(gameService.getMatch(currentUserService.requireCurrentUser(), id));
    }
}
