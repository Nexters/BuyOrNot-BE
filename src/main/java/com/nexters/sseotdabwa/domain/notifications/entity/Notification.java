package com.nexters.sseotdabwa.domain.notifications.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_user_feed_type_target", columnNames = {"user_id", "feed_id", "type", "target_id"})
        },
        indexes = {
                @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_notifications_user_is_read", columnList = "user_id, is_read")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림 수신자
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 관련 피드(투표)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false)
    private boolean isRead;

    @Column
    private LocalDateTime readAt;

    /**
     * 같은 (user, feed, type) 안에서 알림을 구분하는 대상 id. 중복 방지 유니크 키에 포함된다.
     * - 답글 알림(COMMENT_REPLIED): 답글 id → 답글이 달릴 때마다 알림 1건
     * - 그 외 타입: 0 고정 → 기존처럼 (user, feed, type)당 1건
     * NULL은 MySQL 유니크에서 중복 허용되므로 nullable로 두지 않는다.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Builder
    public Notification(User user, Feed feed, String title, String body, NotificationType type, Long targetId) {
        this.user = user;
        this.feed = feed;
        this.title = title;
        this.body = body;
        this.type = type;
        this.targetId = targetId == null ? 0L : targetId;
        this.isRead = false;
        this.readAt = null;
    }

    public void markAsRead() {
        if (this.isRead) return;
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
