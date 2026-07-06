package com.pvc.game.feature.user.dto;

import com.pvc.game.feature.auth.dto.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {
    private UserSummary user;
    private long chips;
}
