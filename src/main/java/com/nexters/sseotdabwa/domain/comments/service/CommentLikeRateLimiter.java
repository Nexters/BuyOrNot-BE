package com.nexters.sseotdabwa.domain.comments.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 비회원 공감 빈도 제한. 비회원 공감은 누를 때마다 공감 수가 올라가므로 연타/스크립트로 숫자를 부풀리는 것을 막는다.
 * 댓글 작성 제한(CommentRateLimiter)과는 별개의 카운터.
 *
 * - 디바이스ID(X-Device-Id)당 분당 10회, IP당 분당 30회. 어느 하나라도 넘으면 차단
 * - X-Device-Id는 선택 — 없으면 IP 기준으로만 제한된다
 * - 같은 와이파이/통신사망에서는 여러 사람이 같은 IP로 보이므로 IP 한도는 디바이스보다 넉넉하게 둔다
 * - 단일 인스턴스(replicas: 1) 전제의 인메모리 카운터. 재배포 시 초기화된다.
 */
@Component
public class CommentLikeRateLimiter {

    static final int DEVICE_LIMIT_PER_MINUTE = 10;
    static final int IP_LIMIT_PER_MINUTE = 30;
    private static final long MAX_CACHE_SIZE = 10_000;

    private final Cache<String, AtomicInteger> ipCounters = buildCache();
    private final Cache<String, AtomicInteger> deviceCounters = buildCache();

    /**
     * @return true면 제한 초과(차단해야 함), false면 통과
     */
    public boolean isExceededForGuest(String ip, String deviceId) {
        if (increment(ipCounters, ip) > IP_LIMIT_PER_MINUTE) {
            return true;
        }
        return increment(deviceCounters, deviceId) > DEVICE_LIMIT_PER_MINUTE;
    }

    private int increment(Cache<String, AtomicInteger> cache, String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }
        return cache.get(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private Cache<String, AtomicInteger> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1).toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(MAX_CACHE_SIZE)
                .build();
    }
}
