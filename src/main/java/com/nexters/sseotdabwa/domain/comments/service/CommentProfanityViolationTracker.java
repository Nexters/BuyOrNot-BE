package com.nexters.sseotdabwa.domain.comments.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.stereotype.Component;

/**
 * 댓글 작성 시 금칙어 위반 누적을 추적한다. 요청 빈도 자체를 제한하는 {@link CommentRateLimiter}와는
 * 별개의 정책/네임스페이스 — 금칙어 위반에만 반응한다(신고 액션에는 적용되지 않음).
 * 10분 내 5회 이상 위반 시, 이후 요청은 검증 이전에 즉시 차단(5분).
 */
@Component
public class CommentProfanityViolationTracker {

    private static final int VIOLATION_THRESHOLD = 5;
    private static final long VIOLATION_WINDOW_MINUTES = 10;
    private static final long BLOCK_MINUTES = 5;
    private static final long MAX_CACHE_SIZE = 10_000;

    private final Cache<String, AtomicInteger> violationCounters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(VIOLATION_WINDOW_MINUTES).toMillis(), TimeUnit.MILLISECONDS)
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    private final Cache<String, Boolean> blockedIdentities = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(BLOCK_MINUTES).toMillis(), TimeUnit.MILLISECONDS)
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    public boolean isBlocked(String identity) {
        return blockedIdentities.getIfPresent(identity) != null;
    }

    /**
     * 금칙어 위반 1건을 기록하고, 10분 내 누적 횟수가 임계치에 도달하면 5분간 차단 상태로 전환한다.
     */
    public void recordViolation(String identity) {
        int count = violationCounters.get(identity, key -> new AtomicInteger(0)).incrementAndGet();
        if (count >= VIOLATION_THRESHOLD) {
            blockedIdentities.put(identity, Boolean.TRUE);
        }
    }
}
