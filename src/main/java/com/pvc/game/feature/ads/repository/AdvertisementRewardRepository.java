package com.pvc.game.feature.ads.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.ads.entity.AdvertisementReward;

public interface AdvertisementRewardRepository extends JpaRepository<AdvertisementReward, UUID> {
}
