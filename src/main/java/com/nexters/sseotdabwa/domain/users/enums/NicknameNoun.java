package com.nexters.sseotdabwa.domain.users.enums;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 랜덤 닉네임 생성에 사용되는 명사(동물) 목록
 * - 형식: {형용사}{명사}_{4자리숫자}
 */
@Getter
@RequiredArgsConstructor
public enum NicknameNoun {
    OTTER("수달"),
    RABBIT("토끼"),
    PENGUIN("펭귄"),
    PANDA("판다"),
    SEAL("물범"),
    QUOKKA("쿼카"),
    FOX("여우"),
    DUCK("오리"),
    SPARROW("참새"),
    SEA_LION("물개"),
    HIPPO("하마"),
    CAMEL("낙타"),
    GIRAFFE("기린"),
    DEER("사슴"),
    BEAVER("비버"),
    LLAMA("라마"),
    CHEETAH("치타"),
    WHALE("고래"),
    MAGPIE("까치"),
    GOOSE("거위");

    private final String displayName;

    private static final NicknameNoun[] CACHED_VALUES = values();

    public static NicknameNoun random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }
}
