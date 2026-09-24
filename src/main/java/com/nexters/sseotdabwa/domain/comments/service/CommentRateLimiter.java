package com.nexters.sseotdabwa.domain.comments.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Component;

/**
 * 댓글 작성 요청 빈도 제한 (IP/디바이스ID 별도 카운트).
 * 콘텐츠 내용과 무관하게 요청 횟수만 카운트한다 — 금칙어 위반 카운터와는 별개의 정책/네임스페이스.
 * 단일 인스턴스(replicas: 1) 배포 구조 전제. replicas가 2 이상으로 늘어나면 Redis 기반으로 재검토 필요.
 */
@Component
public class CommentRateLimiter {

    private static final int LIMIT_PER_WINDOW = 5;
    private static final long WINDOW_MINUTES = 1;
    private static final long MAX_CACHE_SIZE = 10_000;

    private final Cache<String, AtomicInteger> ipCounters = buildCache();
    private final Cache<String, AtomicInteger> deviceCounters = buildCache();

    /**
     * @return true면 제한 초과(차단해야 함), false면 통과
     */
    public boolean isExceeded(String ip, String deviceId) {
        if (increment(ipCounters, ip) > LIMIT_PER_WINDOW) {
            return true;
        }
        return deviceId != null && increment(deviceCounters, deviceId) > LIMIT_PER_WINDOW;
    }

    private int increment(Cache<String, AtomicInteger> cache, String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }
        return cache.get(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private Cache<String, AtomicInteger> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(WINDOW_MINUTES).toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(MAX_CACHE_SIZE)
                .build();
    }
}
