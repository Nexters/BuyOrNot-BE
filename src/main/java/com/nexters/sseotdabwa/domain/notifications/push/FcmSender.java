package com.nexters.sseotdabwa.domain.notifications.push;

import java.util.Map;

/**
 * FCM 전송 추상화
 * - Firebase Admin SDK/HTTP 구현체로 교체 가능
 * - 기본 구현은 Noop(로그만)
 */
public interface FcmSender {

    void send(String fcmToken, String title, String body, Map<String, String> data);
}
