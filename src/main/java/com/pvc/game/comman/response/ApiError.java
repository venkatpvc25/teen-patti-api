package com.pvc.game.comman.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {

    private boolean success;
    private String code;
    private String message;
}
