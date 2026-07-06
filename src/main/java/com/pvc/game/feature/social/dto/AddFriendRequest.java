package com.pvc.game.feature.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddFriendRequest {
    @NotBlank
    private String usernameOrInviteCode;
}
