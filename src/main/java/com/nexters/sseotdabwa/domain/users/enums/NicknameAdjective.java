package com.nexters.sseotdabwa.domain.users.enums;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 랜덤 닉네임 생성에 사용되는 형용사 목록 (소비 심리 테마)
 * - 형식: {형용사}{명사}_{4자리숫자}
 */
@Getter
@RequiredArgsConstructor
public enum NicknameAdjective {
    IMPULSE_POSSESSED("지름신들린"),
    BROKE("텅장된"),
    SALE_ADDICTED("세일못참는"),
    FRUGAL_HACKING("짠테크하는"),
    SALARY_LOOTER("월급루팡인"),
    NO_SPEND_FAILED("무지출실패한"),
    SCORED_DEAL("득템한"),
    CART_ADDING("일단담는"),
    OVERTHINKING_MASTER("고민백단"),
    ABOUT_TO_PAY("결제직전인"),
    SOLD_OUT_SURVIVOR("품절대란겪은"),
    REVIEW_READING("리뷰정독하는"),
    LOWEST_PRICE_HUNTING("최저가찾는"),
    COUPON_COLLECTING("쿠폰모으는"),
    SELF_CONTROLLED("절제하는"),
    CART_FILLING("장바구니채우는"),
    LURKING_ONLY("눈팅만하는"),
    AMBITIOUS("야망충만한"),
    WALLET_CLOSED("지갑닫은"),
    WALLET_OPEN("지갑여는");

    private final String displayName;

    private static final NicknameAdjective[] CACHED_VALUES = values();

    public static NicknameAdjective random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }
}
