package com.nexters.sseotdabwa.api.notifications.dto;

import java.time.LocalDateTime;

import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;

public record NotificationResponse(
        Long notificationId,
        Long feedId,
        NotificationType type,
        String title,
        String body,
        boolean isRead,
        LocalDateTime voteClosedAt,

        Integer resultPercent,
        String resultLabel,
        String viewUrl
) {
    public static NotificationResponse of(
            Notification n,
            LocalDateTime voteClosedAt,
            Integer resultPercent,
            String resultLabel,
            String viewUrl
    ) {
        return new NotificationResponse(
                n.getId(),
                n.getFeed() != null ? n.getFeed().getId() : null,
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                voteClosedAt,
                resultPercent,
                resultLabel,
                viewUrl
        );
    }
}
