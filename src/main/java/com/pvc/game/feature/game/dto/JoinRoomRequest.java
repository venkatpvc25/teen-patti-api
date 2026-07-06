package com.pvc.game.feature.game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinRoomRequest {
    @NotBlank
    private String roomCode;
}
