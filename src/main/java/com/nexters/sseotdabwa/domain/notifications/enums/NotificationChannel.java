package com.nexters.sseotdabwa.domain.notifications.enums;

/**
 * 알림 발송 채널 (push_logs.channel). DB ENUM 컬럼이라 값을 추가하면 수동 ALTER가 필요하다.
 */
public enum NotificationChannel {
    PUSH,   // FCM 푸시 (앱 회원)
    EMAIL   // 이메일 (푸시를 받을 수 없는 웹 회원)
}
