package com.pvc.game.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneOtpRequest {

    @NotBlank
    private String phone;
}
