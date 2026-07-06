package com.pvc.game.feature.ads.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdRewardRequest {
    @NotBlank
    private String placement;
}
