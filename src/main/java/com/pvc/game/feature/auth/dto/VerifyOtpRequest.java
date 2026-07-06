package com.pvc.game.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    private String phone;

    @NotBlank
    @Size(min = 4, max = 8)
    private String otp;

    private String nickname;
    private String avatarUrl;
}
