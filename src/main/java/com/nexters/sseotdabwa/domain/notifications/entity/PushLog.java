package com.nexters.sseotdabwa.domain.notifications.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 푸시(FCM) 발송 시도 로그
 * - Notification(알림함 콘텐츠)과 무관한, 순수 발송 감사(audit) 기록
 * - user/feed는 FK 연관관계 없이 id만 저장 (엔티티 생명주기와 분리)
 */
@Entity
@Table(
        name = "push_logs",
        indexes = {
                @Index(name = "idx_push_logs_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feed_id")
    private Long feedId;

    /**
     * 테스트 푸시처럼 NotificationType에 대응되지 않는 발송은 null
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Builder
    public PushLog(Long userId, Long feedId, NotificationType type, String title, String body,
                   boolean success, String errorMessage) {
        this.userId = userId;
        this.feedId = feedId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
