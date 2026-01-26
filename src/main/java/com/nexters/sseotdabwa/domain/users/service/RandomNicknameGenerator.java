package com.nexters.sseotdabwa.domain.users.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.nexters.sseotdabwa.domain.users.enums.NicknameAdjective;
import com.nexters.sseotdabwa.domain.users.enums.NicknameNoun;

/**
 * 랜덤 닉네임 생성기
 * - 형식: {형용사}{명사}_{4자리숫자}
 */
@Component
public class RandomNicknameGenerator {

    private static final int NUMBER_BOUND = 10000;

    /**
     * 랜덤 닉네임 생성
     * @return 생성된 닉네임
     */
    public String generate() {
        NicknameAdjective adjective = NicknameAdjective.random();
        NicknameNoun noun = NicknameNoun.random();
        int number = ThreadLocalRandom.current().nextInt(NUMBER_BOUND);
        return String.format("%s%s_%04d", adjective.getDisplayName(), noun.getDisplayName(), number);
    }
}
