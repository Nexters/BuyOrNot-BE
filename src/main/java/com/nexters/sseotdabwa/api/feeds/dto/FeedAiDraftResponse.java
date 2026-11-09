package com.nexters.sseotdabwa.api.feeds.dto;

import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;

import io.swagger.v3.oas.annotations.media.Schema;

public record FeedAiDraftResponse(

        @Schema(description = "제목 초안 (40자 이하). 생성 실패/금칙어 포함 시 null", example = "먼작귀 가습기 살까요??ㅜㅜ")
        String title,

        @Schema(description = "내용 초안 (100자 이하). 생성 실패/금칙어 포함 시 null", example = "겨울이라 방이 건조한데 디자인이 너무 귀여워서 고민중..ㅜㅜ 가격대가 좀 있어 보여서 망설여져요")
        String content,

        @Schema(description = "카테고리 초안. 판단이 어려우면 ETC", example = "INTERIOR")
        FeedCategory category,

        @Schema(description = "사진에 원화 가격이 글자로 보일 때만 그 금액(원), 보이지 않거나 원화가 아니면 null. 잘못 읽을 수 있으니 사용자 확인 필요", example = "29000", nullable = true)
        Long price
) {
}
