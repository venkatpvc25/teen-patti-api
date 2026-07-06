package com.pvc.game.feature.store.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRazorpayOrderRequest {

    @NotNull
    private UUID shopItemId;

    @NotBlank
    private String platform;
}
