package com.pvc.game.feature.store.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shop_items")
@Getter
@Setter
public class ShopItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long chips;

    @Column(nullable = false)
    private String priceLabel;

    @Column(nullable = false)
    private long priceAmountPaise;

    @Column(nullable = false)
    private String currency = "INR";

    @Column(nullable = false)
    private boolean vip;

    @Column(nullable = false)
    private boolean active = true;
}
