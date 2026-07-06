package com.pvc.game.feature.game.dto;

import com.pvc.game.feature.game.entity.GameType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMatchRequest {

    @NotNull
    private GameType gameType = GameType.TEEN_PATTI;

    @Min(0)
    private long stake = 0;
}
