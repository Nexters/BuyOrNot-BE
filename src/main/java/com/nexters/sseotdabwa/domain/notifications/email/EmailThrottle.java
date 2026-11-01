package com.nexters.sseotdabwa.domain.notifications.email;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 같은 스레드(원댓글)의 답글 알림 메일은 수신자별로 30분에 1통만 보낸다.
 * - 알림함 저장/앱 푸시는 답글마다 그대로 하고, 이메일만 제한한다 (스팸 처리·Gmail 일일 발송 한도 대응)
 * - 단일 인스턴스(replicas: 1) 전제의 인메모리 캐시. 재시작 시 초기화되며, 수평 확장 시 공유 저장소로 교체 필요
 */
@Component
public class EmailThrottle {

    private static final Duration WINDOW = Duration.ofMinutes(30);
    private static final long MAX_CACHE_SIZE = 10_000;

    private final Cache<String, Boolean> sentKeys = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW)
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    /**
     * 이번에 보내도 되면 true를 반환하고 30분 창을 연다. 창이 열려 있으면 false. (동시 호출에도 1번만 true)
     */
    public boolean tryAcquire(Long userId, Long threadId) {
        return sentKeys.asMap().putIfAbsent(userId + ":" + threadId, Boolean.TRUE) == null;
    }
}
