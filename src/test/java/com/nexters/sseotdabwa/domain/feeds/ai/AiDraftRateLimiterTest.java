package com.nexters.sseotdabwa.domain.feeds.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiDraftRateLimiterTest {

    @Test
    @DisplayName("계정당 분당 5회까지 허용, 6번째부터 차단")
    void limitPerMinute() {
        AiDraftRateLimiter limiter = new AiDraftRateLimiter();

        for (int i = 0; i < AiDraftRateLimiter.LIMIT_PER_MINUTE; i++) {
            assertThat(limiter.isExceeded(1L)).isFalse();
        }
        assertThat(limiter.isExceeded(1L)).isTrue();
    }

    @Test
    @DisplayName("다른 계정의 횟수와 섞이지 않음")
    void separatedByUser() {
        AiDraftRateLimiter limiter = new AiDraftRateLimiter();

        for (int i = 0; i < AiDraftRateLimiter.LIMIT_PER_MINUTE; i++) {
            limiter.isExceeded(1L);
        }
        assertThat(limiter.isExceeded(1L)).isTrue();
        assertThat(limiter.isExceeded(2L)).isFalse();
    }

    @Test
    @DisplayName("비회원 - 같은 IP면 디바이스가 달라도 분당 5회 초과 시 차단")
    void guestLimitedByIp() {
        AiDraftRateLimiter limiter = new AiDraftRateLimiter();

        for (int i = 0; i < AiDraftRateLimiter.LIMIT_PER_MINUTE; i++) {
            assertThat(limiter.isExceededForGuest("1.1.1.1", "device-" + i)).isFalse();
        }
        assertThat(limiter.isExceededForGuest("1.1.1.1", "device-new")).isTrue();
        assertThat(limiter.isExceededForGuest("2.2.2.2", "device-new")).isFalse();
    }

    @Test
    @DisplayName("비회원 - 같은 디바이스면 IP가 바뀌어도 분당 5회 초과 시 차단")
    void guestLimitedByDevice() {
        AiDraftRateLimiter limiter = new AiDraftRateLimiter();

        for (int i = 0; i < AiDraftRateLimiter.LIMIT_PER_MINUTE; i++) {
            assertThat(limiter.isExceededForGuest("10.0.0." + i, "device-1")).isFalse();
        }
        assertThat(limiter.isExceededForGuest("10.0.0.99", "device-1")).isTrue();
    }

    @Test
    @DisplayName("회원 카운터와 비회원 카운터는 섞이지 않음")
    void memberAndGuestSeparated() {
        AiDraftRateLimiter limiter = new AiDraftRateLimiter();

        for (int i = 0; i < AiDraftRateLimiter.LIMIT_PER_MINUTE; i++) {
            limiter.isExceededForGuest("1.1.1.1", null);
        }
        assertThat(limiter.isExceededForGuest("1.1.1.1", null)).isTrue();
        assertThat(limiter.isExceeded(1L)).isFalse();
    }
}
