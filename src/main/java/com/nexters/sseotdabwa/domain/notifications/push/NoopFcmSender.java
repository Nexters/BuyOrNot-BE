package com.nexters.sseotdabwa.domain.notifications.push;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 기본 FCM 전송기 (Noop)
 * - 프로젝트에 Firebase 설정이 아직 없을 때도 컴파일/동작 가능하게 한다.
 * - 실제 푸시 전송은 FcmSender 구현체로
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopFcmSender implements FcmSender {

    @Override
    public void send(String fcmToken, String title, String body, Map<String, String> data) {
        log.info("[NOOP_FCM] token={}, title={}, body={}, data={}", fcmToken, title, body, data);
    }
}
