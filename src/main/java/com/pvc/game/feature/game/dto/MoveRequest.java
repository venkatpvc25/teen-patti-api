package com.pvc.game.feature.game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MoveRequest {
    @NotBlank
    private String action;
    private long chips;
}
