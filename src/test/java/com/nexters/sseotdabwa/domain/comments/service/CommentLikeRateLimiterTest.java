package com.nexters.sseotdabwa.domain.comments.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentLikeRateLimiterTest {

    @Test
    @DisplayName("같은 디바이스는 IP가 달라도 분당 10회를 넘으면 차단")
    void limitedByDevice() {
        CommentLikeRateLimiter limiter = new CommentLikeRateLimiter();

        for (int i = 0; i < CommentLikeRateLimiter.DEVICE_LIMIT_PER_MINUTE; i++) {
            assertThat(limiter.isExceededForGuest("10.0.0." + i, "device-1")).isFalse();
        }
        assertThat(limiter.isExceededForGuest("10.0.0.99", "device-1")).isTrue();
        assertThat(limiter.isExceededForGuest("10.0.0.99", "device-2")).isFalse();
    }

    @Test
    @DisplayName("X-Device-Id가 없으면 IP 기준으로만 제한 (분당 30회)")
    void limitedByIpWithoutDevice() {
        CommentLikeRateLimiter limiter = new CommentLikeRateLimiter();

        for (int i = 0; i < CommentLikeRateLimiter.IP_LIMIT_PER_MINUTE; i++) {
            assertThat(limiter.isExceededForGuest("1.1.1.1", null)).isFalse();
        }
        assertThat(limiter.isExceededForGuest("1.1.1.1", null)).isTrue();
        assertThat(limiter.isExceededForGuest("2.2.2.2", null)).isFalse();
    }

    @Test
    @DisplayName("같은 IP를 쓰는 여러 디바이스는 각자 10회까지, IP 합계 30회를 넘으면 차단")
    void sharedIpAcrossDevices() {
        CommentLikeRateLimiter limiter = new CommentLikeRateLimiter();

        for (int device = 0; device < 3; device++) {
            for (int i = 0; i < CommentLikeRateLimiter.DEVICE_LIMIT_PER_MINUTE; i++) {
                assertThat(limiter.isExceededForGuest("1.1.1.1", "device-" + device)).isFalse();
            }
        }
        assertThat(limiter.isExceededForGuest("1.1.1.1", "device-new")).isTrue();
    }
}
