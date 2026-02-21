package com.nexters.sseotdabwa.domain.notifications.service;

import java.time.LocalDateTime;
import java.util.List;

import com.nexters.sseotdabwa.api.notifications.exception.NotificationErrorCode;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 알림 도메인 서비스
 * - 알림 생성/조회/읽음처리 등 Notification 핵심 로직 담당
 *
 * 원칙:
 * - Notification 테이블이 SSOT
 * - Push는 best-effort (실패해도 DB 롤백하지 않음)
 * - 중복 방지: (user, feed, type)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int RECENT_DAYS = 30;
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    /**
     * 최근 30일 알림 조회 (최신순)
     */
    public List<Notification> getRecentNotifications(Long userId, NotificationType type) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_DAYS);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        if (type == null) {
            return notificationRepository.findRecentByUser(
                    userId,
                    cutoff,
                    pageable
            );
        }

        return notificationRepository.findRecentByUserAndType(
                userId,
                cutoff,
                type,
                pageable
        );
    }

    /**
     * 알림 읽음 처리 (본인 것만)
     * - idempotent
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new GlobalException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    /**
     * (user, feed, type) 중복이면 생성하지 않고 null 반환
     * - pushEnabled=false 여도 Notification은 "저장" 대상이므로 여기서 저장까지 책임진다.
     */
    @Transactional
    public Notification createIfAbsent(User user, Feed feed, NotificationType type, String title, String body) {
        boolean exists = notificationRepository.existsByUserIdAndFeedIdAndType(user.getId(), feed.getId(), type);
        if (exists) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .feed(feed)
                .title(title)
                .body(body)
                .type(type)
                .build();

        return notificationRepository.save(notification);
    }
}
