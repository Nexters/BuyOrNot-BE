package com.nexters.sseotdabwa.domain.users.service;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.nexters.sseotdabwa.domain.users.enums.NicknameAdjective;
import com.nexters.sseotdabwa.domain.users.enums.NicknameNoun;

import static org.assertj.core.api.Assertions.assertThat;

class RandomNicknameGeneratorTest {

    private final RandomNicknameGenerator generator = new RandomNicknameGenerator();

    @Test
    @DisplayName("랜덤 닉네임 생성 - 형식이 올바른지 확인 (형용사+명사_숫자)")
    void generate_returnsCorrectFormat() {
        // when
        String nickname = generator.generate();

        // then
        assertThat(nickname).matches("^.+_\\d{4}$");
    }

    @Test
    @DisplayName("랜덤 닉네임 생성 - 형용사가 유효한 enum 값인지 확인")
    void generate_adjectiveIsValidEnumValue() {
        // when
        String nickname = generator.generate();

        // then
        String prefix = nickname.substring(0, nickname.lastIndexOf("_"));
        boolean hasValidAdjective = Arrays.stream(NicknameAdjective.values())
                .anyMatch(adj -> prefix.startsWith(adj.getDisplayName()));
        assertThat(hasValidAdjective).isTrue();
    }

    @Test
    @DisplayName("랜덤 닉네임 생성 - 명사가 유효한 enum 값인지 확인")
    void generate_nounIsValidEnumValue() {
        // when
        String nickname = generator.generate();

        // then
        String prefix = nickname.substring(0, nickname.lastIndexOf("_"));
        boolean hasValidNoun = Arrays.stream(NicknameNoun.values())
                .anyMatch(noun -> prefix.endsWith(noun.getDisplayName()));
        assertThat(hasValidNoun).isTrue();
    }

    @Test
    @DisplayName("랜덤 닉네임 생성 - 숫자 부분이 0000~9999 범위인지 확인")
    void generate_numberIsInValidRange() {
        // when
        String nickname = generator.generate();

        // then
        String numberPart = nickname.substring(nickname.lastIndexOf("_") + 1);
        int number = Integer.parseInt(numberPart);
        assertThat(number).isBetween(0, 9999);
    }

    @Test
    @DisplayName("isValid - generate()로 만든 닉네임은 항상 유효하다고 판단")
    void isValid_generatedNickname_returnsTrue() {
        // when & then
        for (int i = 0; i < 20; i++) {
            assertThat(generator.isValid(generator.generate())).isTrue();
        }
    }

    @Test
    @DisplayName("isValid - 임의의 문자열은 유효하지 않음")
    void isValid_arbitraryString_returnsFalse() {
        assertThat(generator.isValid("아무말대잔치_1234")).isFalse();
        assertThat(generator.isValid("욕설텍스트")).isFalse();
    }

    @Test
    @DisplayName("isValid - null이면 유효하지 않음")
    void isValid_null_returnsFalse() {
        assertThat(generator.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isValid - 숫자 자리가 4자리가 아니면 유효하지 않음")
    void isValid_wrongNumberLength_returnsFalse() {
        String adjective = NicknameAdjective.values()[0].getDisplayName();
        String noun = NicknameNoun.values()[0].getDisplayName();
        assertThat(generator.isValid(adjective + noun + "_123")).isFalse();
        assertThat(generator.isValid(adjective + noun + "_12345")).isFalse();
    }

    @RepeatedTest(10)
    @DisplayName("랜덤 닉네임 생성 - 여러 번 호출해도 유효한 형식 유지")
    void generate_multipleCallsReturnValidFormat() {
        // when
        String nickname = generator.generate();

        // then
        assertThat(nickname).matches("^.+_\\d{4}$");

        String prefix = nickname.substring(0, nickname.lastIndexOf("_"));
        boolean hasValidAdjective = Arrays.stream(NicknameAdjective.values())
                .anyMatch(adj -> prefix.startsWith(adj.getDisplayName()));
        boolean hasValidNoun = Arrays.stream(NicknameNoun.values())
                .anyMatch(noun -> prefix.endsWith(noun.getDisplayName()));

        assertThat(hasValidAdjective).isTrue();
        assertThat(hasValidNoun).isTrue();
    }
}
