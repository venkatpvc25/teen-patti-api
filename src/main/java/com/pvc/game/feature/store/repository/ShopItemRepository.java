package com.pvc.game.feature.store.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.store.entity.ShopItem;

public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> {
    List<ShopItem> findByActiveTrueOrderByChipsAsc();
}
