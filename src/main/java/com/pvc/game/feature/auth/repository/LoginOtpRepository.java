package com.pvc.game.feature.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.auth.entity.LoginOtp;

public interface LoginOtpRepository extends JpaRepository<LoginOtp, UUID> {
    Optional<LoginOtp> findFirstByPhoneAndUsedFalseOrderByCreatedAtDesc(String phone);
}
