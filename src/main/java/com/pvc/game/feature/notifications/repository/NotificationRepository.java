package com.pvc.game.feature.notifications.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.notifications.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop50ByUserOrderByCreatedAtDesc(User user);
}
