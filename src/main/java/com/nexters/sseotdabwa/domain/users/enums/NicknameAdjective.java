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
    ANGRY("화가난"),
    CAREFUL("신중한"),
    IMPULSIVE("즉흥적"),
    THRIFTY("알뜰한"),
    HESITANT("소심한"),
    DECISIVE("과감한"),
    WISE("현명한"),
    GREEDY("욕심난"),
    CURIOUS("궁금한"),
    EXCITED("설레는"),
    HAPPY("행복한"),
    DREAMY("몽글한"),
    SLEEPY("졸린"),
    HUNGRY("배고픈"),
    PROUD("뿌듯한"),
    LUCKY("운좋은"),
    SPARKLY("빛나는"),
    FLUFFY("포근한"),
    PLAYFUL("발랄한"),
    CUTE("귀여운");

    private final String displayName;

    private static final NicknameAdjective[] CACHED_VALUES = values();

    public static NicknameAdjective random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }
}
