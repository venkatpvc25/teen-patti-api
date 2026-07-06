package com.pvc.game.feature.notifications.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.notifications.entity.Notification;
import com.pvc.game.feature.notifications.repository.NotificationRepository;
import com.pvc.game.feature.user.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final CurrentUserService currentUserService;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public ApiResponse<List<Notification>> notifications() {
        return ApiResponse.ok(notificationRepository.findTop50ByUserOrderByCreatedAtDesc(
                currentUserService.requireCurrentUser()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Notification> markRead(@PathVariable UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        return ApiResponse.ok(notificationRepository.save(notification));
    }
}
