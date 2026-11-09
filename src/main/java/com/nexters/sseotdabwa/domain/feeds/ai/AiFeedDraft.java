package com.nexters.sseotdabwa.domain.feeds.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;

/**
 * AI가 생성한 피드 글 초안 (Claude 구조화 출력의 JSON 스키마로도 사용)
 * - 구조화 출력은 모든 필드가 required라 null을 낼 수 없으므로, 가격을 모르면 0으로 받고 호출하는 쪽에서 null로 바꾼다
 */
public record AiFeedDraft(
        @JsonPropertyDescription("투표 글 제목. 40자 이하 한 줄")
        String title,

        @JsonPropertyDescription("투표 글 내용. 100자 이하, 1~3문장")
        String content,

        @JsonPropertyDescription("상품 카테고리. 애매하면 ETC")
        FeedCategory category,

        @JsonPropertyDescription("사진에 원화 가격이 글자로 보이면 그 금액(원 단위 정수), 보이지 않거나 원화가 아니면 0")
        Long price
) {
}
