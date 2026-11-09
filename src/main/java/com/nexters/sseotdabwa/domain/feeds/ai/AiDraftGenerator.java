package com.nexters.sseotdabwa.domain.feeds.ai;

import java.util.List;

/**
 * 상품 사진 → 피드(투표) 글 초안 생성 추상화
 * - 현재 구현은 Claude API(ClaudeAiDraftGenerator). API 키가 없는 환경(local/test 등)에서는 NoopAiDraftGenerator가 동작한다.
 * - 생성 실패/거절/타임아웃 시 GlobalException(FEED_AI_DRAFT_FAILED)을 던진다.
 * - 반환값은 모델 출력 그대로이므로 길이/금칙어 등 최종 검증은 호출하는 쪽에서 한다.
 */
public interface AiDraftGenerator {

    /**
     * @param imageUrls 공개 접근 가능한 이미지 URL (1~3장)
     */
    AiFeedDraft generate(List<String> imageUrls);
}
