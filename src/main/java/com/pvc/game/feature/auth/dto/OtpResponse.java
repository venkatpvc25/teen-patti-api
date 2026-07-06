package com.pvc.game.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpResponse {
    private String phone;
    private int expiresInSeconds;
    private String message;
}
