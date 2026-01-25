package com.nexters.sseotdabwa.api.auth.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenResponse;
import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.auth.service.KakaoOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.dto.KakaoUserInfo;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.service.UserService;
import com.nexters.sseotdabwa.domain.users.service.command.UserCreateCommand;

import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 비즈니스 로직을 조합하는 Facade
 * - 카카오 로그인: 카카오 사용자 정보 조회 → 회원가입/로그인 → JWT 발급
 * - 토큰 갱신: Refresh Token 검증 → 새 Access Token 발급
 */
@Component
@RequiredArgsConstructor
@Transactional
public class AuthFacade {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    /**
     * 카카오 소셜 로그인
     * 1. 카카오 Access Token으로 사용자 정보 조회
     * 2. 기존 회원이면 프로필 업데이트, 신규 회원이면 가입 처리
     * 3. JWT Access/Refresh Token 발급
     */
    public TokenResponse loginWithKakao(KakaoLoginRequest request) {
        // 카카오 API로 사용자 정보 조회
        KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(request.accessToken());

        // 기존 회원 조회 또는 신규 가입
        User user = userService.findBySocialIdAndProvider(kakaoUserInfo.getId(), SocialAccount.KAKAO)
                .orElseGet(() -> userService.createUser(
                        new UserCreateCommand(
                                kakaoUserInfo.getId(),
                                kakaoUserInfo.getEmail(),
                                kakaoUserInfo.getNickname(),
                                SocialAccount.KAKAO,
                                kakaoUserInfo.getProfileImage()
                        )
                ));

        // 카카오에서 변경된 프로필 정보 동기화
        userService.updateProfile(user, kakaoUserInfo.getNickname(), kakaoUserInfo.getProfileImage());

        // JWT 토큰 발급
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        String refreshToken = jwtTokenService.createRefreshToken(user.getId());

        return new TokenResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    /**
     * Access Token 갱신
     * - Refresh Token이 유효하면 새 Access Token 발급
     * - Refresh Token은 그대로 유지
     */
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        if (!jwtTokenService.validateToken(request.refreshToken())) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenService.getUserIdFromToken(request.refreshToken());
        User user = userService.findById(userId);

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        return new TokenResponse(accessToken, request.refreshToken(), "Bearer", UserResponse.from(user));
    }
}
