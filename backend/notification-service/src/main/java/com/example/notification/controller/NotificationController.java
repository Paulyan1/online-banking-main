package com.example.notification.controller;

import com.example.notification.dto.NotificationDto.*;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PagedNotificationsResponse> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                notificationService.getMyNotifications(getSubject(jwt), page, size));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(getSubject(jwt));
        return ResponseEntity.noContent().build();
    }

    // Helper method to extract user ID from JWT
    private String getSubject(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "local-test-user";
    }
}
