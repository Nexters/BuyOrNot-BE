package com.nexters.sseotdabwa.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JwtTokenServiceTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("AccessToken 생성 성공")
    void createAccessToken_success() {
        // given
        Long userId = 1L;

        // when
        String token = jwtTokenService.createAccessToken(userId);

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.validateToken(token)).isTrue();
        assertThat(jwtTokenService.getUserIdFromToken(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("RefreshToken 생성 성공")
    void createRefreshToken_success() {
        // given
        Long userId = 1L;

        // when
        String token = jwtTokenService.createRefreshToken(userId);

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.validateToken(token)).isTrue();
        assertThat(jwtTokenService.getUserIdFromToken(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    void validateToken_malformed_returnsFalse() {
        // given
        String malformedToken = "invalid.token.format";

        // when
        boolean result = jwtTokenService.validateToken(malformedToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_empty_returnsFalse() {
        // given
        String emptyToken = "";

        // when
        boolean result = jwtTokenService.validateToken(emptyToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null 토큰 검증 실패")
    void validateToken_null_returnsFalse() {
        // when
        boolean result = jwtTokenService.validateToken(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰에서 userId 추출 성공")
    void getUserIdFromToken_success() {
        // given
        Long userId = 123L;
        String token = jwtTokenService.createAccessToken(userId);

        // when
        Long extractedUserId = jwtTokenService.getUserIdFromToken(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("RefreshToken 검증 성공")
    void validateRefreshToken_success() {
        // given
        Long userId = 1L;
        String refreshToken = jwtTokenService.createRefreshToken(userId);

        // when
        boolean result = jwtTokenService.validateRefreshToken(refreshToken);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("AccessToken으로 RefreshToken 검증 시 실패")
    void validateRefreshToken_withAccessToken_returnsFalse() {
        // given
        Long userId = 1L;
        String accessToken = jwtTokenService.createAccessToken(userId);

        // when
        boolean result = jwtTokenService.validateRefreshToken(accessToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰으로 RefreshToken 검증 시 실패")
    void validateRefreshToken_malformed_returnsFalse() {
        // given
        String malformedToken = "invalid.token.format";

        // when
        boolean result = jwtTokenService.validateRefreshToken(malformedToken);

        // then
        assertThat(result).isFalse();
    }
}
