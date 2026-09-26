package com.nexters.sseotdabwa.domain.comments.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Component;

/**
 * 댓글 작성 요청 빈도 제한.
 * 콘텐츠 내용과 무관하게 요청 횟수만 카운트한다 — 금칙어 위반 카운터와는 별개의 정책/네임스페이스.
 * 단일 인스턴스(replicas: 1) 배포 구조 전제. replicas가 2 이상으로 늘어나면 Redis 기반으로 재검토 필요.
 *
 * 회원과 게스트는 식별 기준이 다르다:
 * - 회원: 계정(userId) 기준. 웹 회원 요청은 Bearer 토큰을 붙이는 Cloudflare Worker를 거쳐 백엔드로 들어오기 때문에
 *   모든 웹 회원의 요청이 동일한 IP로 보인다 — IP 기준으로 제한하면 회원 전체가 하나의 한도를 나눠 쓰게 되어버림.
 * - 게스트: IP + 디바이스ID 기준(기존 방식 유지). 게스트 요청은 브라우저가 백엔드를 직접 호출해 실제 IP가 보임.
 */
@Component
public class CommentRateLimiter {

    private static final int LIMIT_PER_WINDOW = 5;
    private static final long WINDOW_MINUTES = 1;
    private static final long MAX_CACHE_SIZE = 10_000;

    private final Cache<String, AtomicInteger> ipCounters = buildCache();
    private final Cache<String, AtomicInteger> deviceCounters = buildCache();
    private final Cache<Long, AtomicInteger> userIdCounters = buildCache();

    /**
     * 게스트 댓글 작성용 — IP/디바이스ID 기준.
     * @return true면 제한 초과(차단해야 함), false면 통과
     */
    public boolean isExceeded(String ip, String deviceId) {
        if (increment(ipCounters, ip) > LIMIT_PER_WINDOW) {
            return true;
        }
        return deviceId != null && increment(deviceCounters, deviceId) > LIMIT_PER_WINDOW;
    }

    /**
     * 회원 댓글 작성용 — 계정(userId) 기준. IP는 프록시(Cloudflare Worker)로 인해 신뢰할 수 없어 사용하지 않는다.
     * @return true면 제한 초과(차단해야 함), false면 통과
     */
    public boolean isExceededForMember(Long userId) {
        return increment(userIdCounters, userId) > LIMIT_PER_WINDOW;
    }

    private <K> int increment(Cache<K, AtomicInteger> cache, K key) {
        if (key == null || (key instanceof String s && s.isBlank())) {
            return 0;
        }
        return cache.get(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private <K> Cache<K, AtomicInteger> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(WINDOW_MINUTES).toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(MAX_CACHE_SIZE)
                .build();
    }
}
