package com.nexters.sseotdabwa.domain.users.service;

import java.util.Arrays;
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

    /**
     * 닉네임이 실제 단어 목록 조합({형용사}{명사}_{4자리숫자})인지 검증
     * - 게스트가 API를 조작해 임의 문자열을 닉네임으로 보내는 것을 막기 위함
     */
    public boolean isValid(String nickname) {
        if (nickname == null || !nickname.matches("^.+_\\d{4}$")) {
            return false;
        }

        String prefix = nickname.substring(0, nickname.lastIndexOf("_"));
        boolean hasValidAdjective = Arrays.stream(NicknameAdjective.values())
                .anyMatch(adjective -> prefix.startsWith(adjective.getDisplayName()));
        boolean hasValidNoun = Arrays.stream(NicknameNoun.values())
                .anyMatch(noun -> prefix.endsWith(noun.getDisplayName()));

        return hasValidAdjective && hasValidNoun;
    }
}
