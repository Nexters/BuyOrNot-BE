package com.nexters.sseotdabwa.domain.feeds.ai;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * API 키(app.ai.api-key)가 없는 환경용 — 기능 비활성 에러를 내려 클라이언트가 직접 입력 화면을 유지하도록 한다
 */
@Slf4j
@Component
@ConditionalOnExpression("'${app.ai.api-key:}' == ''")
public class NoopAiDraftGenerator implements AiDraftGenerator {

    @Override
    public AiFeedDraft generate(List<String> imageUrls) {
        log.info("[NOOP_AI_DRAFT] images={}", imageUrls.size());
        throw new GlobalException(FeedErrorCode.FEED_AI_DRAFT_UNAVAILABLE);
    }
}
