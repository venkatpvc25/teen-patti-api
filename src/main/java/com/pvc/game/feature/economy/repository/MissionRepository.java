package com.pvc.game.feature.economy.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.economy.entity.Mission;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    List<Mission> findByActiveTrue();
}
