package com.pvc.game.feature.auth.dto;

import java.util.UUID;

import com.pvc.game.feature.auth.entity.Role;
import com.pvc.game.feature.auth.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummary {

    private UUID id;
    private String username;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private int level;
    private long xp;
    private Role role;

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getLevel(),
                user.getXp(),
                user.getRole());
    }
}
