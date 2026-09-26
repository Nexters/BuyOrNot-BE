package com.nexters.sseotdabwa.domain.comments.service;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 의존성이 없는 순수 컴포넌트라 Spring 컨텍스트 없이 직접 테스트한다.
 * Rate Limiting과 완전히 별개의 카운터임을 이 레벨에서 격리해 검증 — 회원 rate limit이
 * userId 기준으로 바뀌면서 Facade 레벨 테스트에서는 두 정책이 같이 걸려 더 이상 분리 검증이 어려움.
 */
class CommentProfanityViolationTrackerTest {

    private final CommentProfanityViolationTracker tracker = new CommentProfanityViolationTracker();

    @Test
    @DisplayName("위반 4회까지는 차단되지 않는다")
    void recordViolation_underThreshold_notBlocked() {
        // given
        String identity = uniqueIdentity();

        // when
        for (int i = 0; i < 4; i++) {
            tracker.recordViolation(identity);
        }

        // then
        assertThat(tracker.isBlocked(identity)).isFalse();
    }

    @Test
    @DisplayName("10분 내 위반 5회 누적 시 차단 상태로 전환된다")
    void recordViolation_reachesThreshold_becomesBlocked() {
        // given
        String identity = uniqueIdentity();

        // when
        for (int i = 0; i < 5; i++) {
            tracker.recordViolation(identity);
        }

        // then
        assertThat(tracker.isBlocked(identity)).isTrue();
    }

    @Test
    @DisplayName("서로 다른 identity는 위반 횟수가 섞이지 않는다")
    void recordViolation_isIsolatedPerIdentity() {
        // given
        String identityA = uniqueIdentity();
        String identityB = uniqueIdentity();

        // when: A만 5회 위반
        for (int i = 0; i < 5; i++) {
            tracker.recordViolation(identityA);
        }

        // then
        assertThat(tracker.isBlocked(identityA)).isTrue();
        assertThat(tracker.isBlocked(identityB)).isFalse();
    }

    private String uniqueIdentity() {
        return "user:" + UUID.randomUUID();
    }
}
