package com.nexters.sseotdabwa.domain.feeds.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;

class NoopAiDraftGeneratorTest {

    @Test
    @DisplayName("API 키가 없는 환경 - 기능 비활성 에러(FEED_018)")
    void generate_unavailable() {
        assertThatThrownBy(() -> new NoopAiDraftGenerator().generate(List.of("https://cdn/feeds/a.jpg")))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_UNAVAILABLE);
    }
}
