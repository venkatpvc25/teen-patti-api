package com.pvc.game.feature.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawalRequest {

    @NotNull
    @Min(1)
    private Long chips;

    private String upiId;
    private String razorpayFundAccountId;
}
