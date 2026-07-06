package com.pvc.game.feature.social.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.auth.dto.UserSummary;
import com.pvc.game.feature.auth.repository.UserRepository;
import com.pvc.game.feature.social.dto.AddFriendRequest;
import com.pvc.game.feature.social.entity.Friend;
import com.pvc.game.feature.social.repository.FriendRepository;
import com.pvc.game.feature.user.service.CurrentUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    @GetMapping
    public ApiResponse<List<UserSummary>> friends() {
        var user = currentUserService.requireCurrentUser();
        return ApiResponse.ok(friendRepository.findByUser(user).stream()
                .map(friend -> UserSummary.from(friend.getFriendUser()))
                .toList());
    }

    @PostMapping
    public ApiResponse<UserSummary> addFriend(@Valid @RequestBody AddFriendRequest request) {
        var user = currentUserService.requireCurrentUser();
        var friendUser = userRepository.findByUsername(request.getUsernameOrInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));
        if (!friendRepository.existsByUserAndFriendUser(user, friendUser)) {
            Friend friend = new Friend();
            friend.setUser(user);
            friend.setFriendUser(friendUser);
            friendRepository.save(friend);
        }
        return ApiResponse.ok(UserSummary.from(friendUser));
    }
}
