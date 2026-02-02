package com.nexters.sseotdabwa.domain.feeds.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 피드 카테고리
 * - 피드 분류 및 필터링에 사용
 */
@Getter
@RequiredArgsConstructor
public enum FeedCategory {

    LUXURY("명품·프리미엄"),
    FASHION("패션·잡화"),
    BEAUTY("화장품·뷰티"),
    TREND("트렌드·가성비템"),
    FOOD("음식"),
    ELECTRONICS("전자기기"),
    TRAVEL("여행 쇼핑템"),
    HEALTH("헬스·운동용품"),
    BOOK("도서"),
    ETC("기타");

    private final String displayName;
}
