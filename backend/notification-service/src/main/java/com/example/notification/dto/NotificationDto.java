package com.example.notification.dto;

import com.example.notification.model.Notification;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.processing.Generated;

public class NotificationDto {
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationResponse {
        private Long id;
        private String userId;
        private Notification.NotificationType type;
        private String title;
        private String message;
        private boolean read;
        private String referenceId;
        private LocalDateTime createdAt;

        public static NotificationResponse from(Notification n) {
            return NotificationResponse.builder()
                    .id(n.getId())
                    .userId(n.getUserId())
                    .type(n.getType())
                    .title(n.getTitle())
                    .message(n.getMessage())
                    .read(n.isRead())
                    .referenceId(n.getReferenceId())
                    .createdAt(n.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagedNotificationsResponse {
        private List<NotificationResponse> notifications;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private long unreadCount;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebSocketNotification {
        private Long id;
        private Notification.NotificationType type;
        private String title;
        private String message;
        private String referenceId;
        private LocalDateTime createdAt;

        public static WebSocketNotification from(Notification n) {
            return WebSocketNotification.builder()
                    .id(n.getId())
                    .type(n.getType())
                    .title(n.getTitle())
                    .message(n.getMessage())
                    .referenceId(n.getReferenceId())
                    .createdAt(n.getCreatedAt())
                    .build();
        }
    }

}
