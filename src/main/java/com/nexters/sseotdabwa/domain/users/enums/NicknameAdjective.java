package com.nexters.sseotdabwa.domain.users.enums;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 랜덤 닉네임 생성에 사용되는 형용사 목록
 * - 형식: {형용사}{명사}_{4자리숫자}
 */
@Getter
@RequiredArgsConstructor
public enum NicknameAdjective {
    PONDERING("고민하는"),
    CAREFUL("신중한"),
    IMPULSIVE("충동적인"),
    THRIFTY("알뜰한"),
    HESITANT("망설이는"),
    DECISIVE("저지르는"),
    WISE("현명한"),
    GREEDY("욕심많은"),
    CURIOUS("궁금한"),
    EXCITED("설레는"),
    HAPPY("행복한"),
    DREAMY("몽글몽글"),
    SLEEPY("졸린"),
    HUNGRY("배고픈"),
    PROUD("뿌듯한"),
    LUCKY("운좋은"),
    SPARKLY("반짝이는"),
    FLUFFY("포근한"),
    PLAYFUL("장난꾸러기"),
    CUTE("귀여운");

    private final String displayName;

    private static final NicknameAdjective[] CACHED_VALUES = values();

    public static NicknameAdjective random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }
}
