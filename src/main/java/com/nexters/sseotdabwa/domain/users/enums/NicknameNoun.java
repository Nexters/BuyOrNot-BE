package com.nexters.sseotdabwa.domain.users.enums;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 랜덤 닉네임 생성에 사용되는 명사(소비 페르소나) 목록
 * - 형식: {형용사}{명사}_{4자리숫자}
 */
@Getter
@RequiredArgsConstructor
public enum NicknameNoun {
    SHOPPING_GHOST("지름신"),
    THRIFTY_HOMEMAKER("알뜰살림꾼"),
    STINGY_GIRL("짠순이"),
    STINGY_GUY("짠돌이"),
    SHOPPING_ADDICT("쇼핑덕후"),
    DEAL_SCORER("득템러"),
    EARLY_BIRD("얼리버드"),
    NIGHT_SHOPPER("야행성쇼퍼"),
    SEARCH_KING("검색왕"),
    COMPARE_KING("비교왕"),
    REVIEW_MASTER("후기장인"),
    SMALL_JOY_SEEKER("소확행러"),
    OVERTHINK_KING("고민왕"),
    SALE_HUNTER("세일헌터"),
    VALUE_KING("가성비왕"),
    LUXURY_HUNTER("명품헌터"),
    SPENDING_GENIUS("소비천재"),
    SPENDING_FAIRY("지출요정"),
    NOTIFICATION_BOT("지출천재"),
    NO_SPEND_CHALLENGER("무지출챌린저");

    private final String displayName;

    private static final NicknameNoun[] CACHED_VALUES = values();

    public static NicknameNoun random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }
}
