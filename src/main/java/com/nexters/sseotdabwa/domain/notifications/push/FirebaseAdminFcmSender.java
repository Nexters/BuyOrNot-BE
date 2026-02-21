package com.nexters.sseotdabwa.domain.notifications.push;

import java.util.Map;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Firebase Admin SDK 기반 FCM Sender
 * - FirebaseApp Bean이 존재할 때만 등록된다.
 * - 그렇지 않으면 NoopFcmSender가 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseAdminFcmSender implements FcmSender {

    private final FirebaseApp firebaseApp;

    @Override
    public void send(String fcmToken, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data == null ? Map.of() : data)
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance(firebaseApp).send(message);
            log.info("FCM sent. messageId={}, tokenPrefix={}", messageId, tokenPrefix(fcmToken));
        } catch (Exception e) {
            // best-effort: 상위에서 롤백하지 않도록 예외를 던지지 않고 로그만 남김
            log.warn("FCM send failed. tokenPrefix={}", tokenPrefix(fcmToken), e);
        }
    }

    private String tokenPrefix(String token) {
        if (token == null) return "null";
        return token.length() <= 10 ? token : token.substring(0, 10) + "...";
    }
}
