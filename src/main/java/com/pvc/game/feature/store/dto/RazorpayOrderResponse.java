package com.pvc.game.feature.store.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RazorpayOrderResponse {
    private UUID purchaseId;
    private String razorpayKeyId;
    private String razorpayOrderId;
    private long amountPaise;
    private String currency;
    private String itemName;
    private long chips;
}
