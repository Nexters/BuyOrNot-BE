package com.nexters.sseotdabwa.domain.users.enums;

import java.util.Random;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 랜덤 닉네임 생성에 사용되는 명사(동물) 목록
 * - 형식: {형용사}{명사}_{4자리숫자}
 */
@Getter
@RequiredArgsConstructor
public enum NicknameNoun {
    HAMSTER("햄스터"),
    SQUIRREL("다람쥐"),
    HEDGEHOG("고슴도치"),
    OTTER("수달"),
    RABBIT("토끼"),
    CAT("고양이"),
    PUPPY("강아지"),
    PENGUIN("펭귄"),
    PANDA("판다"),
    BEAR("곰돌이"),
    SEAL("물범"),
    QUOKKA("쿼카"),
    ALPACA("알파카"),
    CAPYBARA("카피바라"),
    RACCOON("너구리"),
    FOX("여우"),
    KOALA("코알라"),
    SLOTH("나무늘보"),
    DUCKLING("아기오리"),
    CHICK("병아리");

    private final String displayName;

    private static final Random RANDOM = new Random();

    public static NicknameNoun random() {
        NicknameNoun[] values = values();
        return values[RANDOM.nextInt(values.length)];
    }
}
