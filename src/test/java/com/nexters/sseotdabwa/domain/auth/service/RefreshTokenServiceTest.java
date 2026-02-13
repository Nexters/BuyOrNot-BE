package com.nexters.sseotdabwa.domain.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.auth.entity.RefreshToken;
import com.nexters.sseotdabwa.domain.auth.repository.RefreshTokenRepository;
import com.nexters.sseotdabwa.domain.auth.service.command.RefreshTokenCreateCommand;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Refresh Token 저장 성공")
    void save_success() {
        // given
        RefreshTokenCreateCommand command = new RefreshTokenCreateCommand(
                1L, "test-token-value", LocalDateTime.now().plusDays(14)
        );

        // when
        RefreshToken saved = refreshTokenService.save(command);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getToken()).isEqualTo("test-token-value");
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("토큰으로 Refresh Token 조회 성공")
    void findByToken_success() {
        // given
        RefreshToken token = refreshTokenRepository.save(RefreshToken.builder()
                .userId(1L)
                .token("find-me-token")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        // when
        Optional<RefreshToken> found = refreshTokenService.findByToken("find-me-token");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(token.getId());
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 시 빈 Optional 반환")
    void findByToken_notFound_returnsEmpty() {
        // when
        Optional<RefreshToken> found = refreshTokenService.findByToken("nonexistent-token");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("토큰으로 Refresh Token 삭제 성공")
    void deleteByToken_success() {
        // given
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(1L)
                .token("delete-me-token")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        // when
        refreshTokenService.deleteByToken("delete-me-token");

        // then
        assertThat(refreshTokenRepository.findByToken("delete-me-token")).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 Refresh Token 전체 삭제 성공")
    void deleteByUserId_success() {
        // given
        Long userId = 100L;
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token("user-token-1")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token("user-token-2")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        // when
        refreshTokenService.deleteByUserId(userId);

        // then
        assertThat(refreshTokenRepository.findByToken("user-token-1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("user-token-2")).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 삭제 시 다른 사용자의 토큰은 영향 없음")
    void deleteByUserId_doesNotAffectOtherUsers() {
        // given
        Long userId = 100L;
        Long otherUserId = 200L;
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token("user-token")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(otherUserId)
                .token("other-user-token")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        // when
        refreshTokenService.deleteByUserId(userId);

        // then
        assertThat(refreshTokenRepository.findByToken("user-token")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("other-user-token")).isPresent();
    }

    @Test
    @DisplayName("한 사용자가 여러 Refresh Token을 보유할 수 있음")
    void multipleTokensPerUser() {
        // given
        Long userId = 1L;
        RefreshTokenCreateCommand command1 = new RefreshTokenCreateCommand(
                userId, "multi-token-1", LocalDateTime.now().plusDays(14)
        );
        RefreshTokenCreateCommand command2 = new RefreshTokenCreateCommand(
                userId, "multi-token-2", LocalDateTime.now().plusDays(14)
        );

        // when
        refreshTokenService.save(command1);
        refreshTokenService.save(command2);

        // then
        assertThat(refreshTokenService.findByToken("multi-token-1")).isPresent();
        assertThat(refreshTokenService.findByToken("multi-token-2")).isPresent();
    }
}
