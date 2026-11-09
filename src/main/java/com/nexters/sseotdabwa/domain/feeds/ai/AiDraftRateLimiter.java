package com.nexters.sseotdabwa.domain.feeds.ai;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * AI 초안 생성 요청 빈도 제한 (외부 API 비용 어뷰징 방지). 분당 5회 + 하루 20회.
 * 실패한 요청도 외부 호출 비용이 들 수 있어 동일하게 카운트한다.
 * 단일 인스턴스(replicas: 1) 전제의 인메모리 카운터. 재배포 시 초기화된다.
 *
 * 회원과 비회원은 식별 기준이 다르다 (CommentRateLimiter와 같은 이유):
 * - 회원: 계정(userId) 기준. 웹 회원 요청은 Cloudflare Worker를 거쳐 IP가 모두 같으므로 IP를 쓰지 않는다.
 * - 비회원: IP와 디바이스ID(X-Device-Id) 각각 기준. 비회원 요청은 브라우저가 백엔드를 직접 호출해 실제 IP가 보인다.
 */
@Component
public class AiDraftRateLimiter {

    static final int LIMIT_PER_MINUTE = 5;
    static final int LIMIT_PER_DAY = 20;
    private static final long MAX_CACHE_SIZE = 10_000;

    private final Cache<Long, AtomicInteger> userMinuteCounters = buildCache(Duration.ofMinutes(1));
    private final Cache<Long, AtomicInteger> userDayCounters = buildCache(Duration.ofDays(1));
    private final Cache<String, AtomicInteger> guestMinuteCounters = buildCache(Duration.ofMinutes(1));
    private final Cache<String, AtomicInteger> guestDayCounters = buildCache(Duration.ofDays(1));

    /**
     * 회원용 — 계정(userId) 기준.
     * @return true면 제한 초과(차단해야 함), false면 통과
     */
    public boolean isExceeded(Long userId) {
        if (userId == null) {
            return false;
        }
        return isExceeded(userMinuteCounters, userDayCounters, userId);
    }

    /**
     * 비회원용 — IP와 디바이스ID를 각각 카운트해 어느 하나라도 넘으면 차단.
     * @return true면 제한 초과(차단해야 함), false면 통과
     */
    public boolean isExceededForGuest(String ip, String deviceId) {
        if (hasText(ip) && isExceeded(guestMinuteCounters, guestDayCounters, "ip:" + ip)) {
            return true;
        }
        return hasText(deviceId) && isExceeded(guestMinuteCounters, guestDayCounters, "device:" + deviceId);
    }

    private <K> boolean isExceeded(Cache<K, AtomicInteger> minuteCounters, Cache<K, AtomicInteger> dayCounters, K key) {
        if (increment(minuteCounters, key) > LIMIT_PER_MINUTE) {
            return true;
        }
        return increment(dayCounters, key) > LIMIT_PER_DAY;
    }

    private <K> int increment(Cache<K, AtomicInteger> cache, K key) {
        return cache.get(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <K> Cache<K, AtomicInteger> buildCache(Duration window) {
        return Caffeine.newBuilder()
                .expireAfterWrite(window.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(MAX_CACHE_SIZE)
                .build();
    }
}
