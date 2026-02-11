package com.nexters.sseotdabwa.api.auth.facade;

import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.auth.dto.AppleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.GoogleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenResponse;
import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.AppleOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.GoogleOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.auth.service.KakaoOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.RefreshTokenService;
import com.nexters.sseotdabwa.domain.auth.service.command.RefreshTokenCreateCommand;
import com.nexters.sseotdabwa.domain.auth.service.external.AppleUserInfo;
import com.nexters.sseotdabwa.domain.auth.service.external.GoogleUserInfo;
import com.nexters.sseotdabwa.domain.auth.service.external.KakaoUserInfo;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.DefaultProfileImage;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.service.UserService;
import com.nexters.sseotdabwa.domain.users.service.command.UserCreateCommand;

import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 비즈니스 로직을 조합하는 Facade
 * - 카카오 로그인: 카카오 사용자 정보 조회 → 회원가입/로그인 → JWT 발급
 * - Google 로그인: Google ID Token 검증 → 회원가입/로그인 → JWT 발급
 * - 토큰 갱신: Refresh Token 검증 → 새 Access Token 발급
 */
@Component
@RequiredArgsConstructor
@Transactional
public class AuthFacade {

    private final KakaoOAuthService kakaoOAuthService;
    private final GoogleOAuthService googleOAuthService;
    private final AppleOAuthService appleOAuthService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    /**
     * CloudFront 도메인
     */
    @Value("${aws.cloudfront.domain}")
    private String cloudfrontDomain;

    /**
     * 카카오 소셜 로그인
     * 1. 카카오 Access Token으로 사용자 정보 조회 (socialId만 사용)
     * 2. 기존 회원이면 로그인, 신규 회원이면 랜덤 닉네임/프로필로 가입 처리
     * 3. JWT Access/Refresh Token 발급
     */
    public TokenResponse loginWithKakao(KakaoLoginRequest request) {
        // 카카오 API로 사용자 정보 조회 (socialId 확인용)
        KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(request.accessToken());

        // 기존 회원 조회 또는 신규 가입 (신규 회원은 랜덤 닉네임/프로필 이미지 부여)
        String socialId = String.valueOf(kakaoUserInfo.getId());
        String email = kakaoUserInfo.getEmail();
        User user = userService.findBySocialIdAndProvider(socialId, SocialAccount.KAKAO)
                .map(existingUser -> {
                    if (email != null && !email.isBlank()) {
                        existingUser.updateEmail(email);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userService.createUser(
                        new UserCreateCommand(
                                socialId,
                                userService.generateUniqueNickname(),
                                SocialAccount.KAKAO,
                                randomDefaultProfileImageUrl(),
                                email
                        )
                ));

        return createTokenResponse(user);
    }

    /**
     * Apple 소셜 로그인
     * 1. Authorization Code를 Apple Token API로 교환하여 사용자 정보 추출 (socialId만 사용)
     * 2. 기존 회원이면 로그인, 신규 회원이면 랜덤 닉네임/프로필로 가입 처리
     * 3. JWT Access/Refresh Token 발급
     */
    public TokenResponse loginWithApple(AppleLoginRequest request) {
        // Apple Authorization Code로 사용자 정보 조회 (socialId 확인용)
        AppleUserInfo appleUserInfo = appleOAuthService.getAppleUserInfo(request.authorizationCode(), request.redirectUri());

        // 기존 회원 조회 또는 신규 가입 (신규 회원은 랜덤 닉네임/프로필 이미지 부여)
        String socialId = appleUserInfo.getSub();
        String email = appleUserInfo.getEmail();
        User user = userService.findBySocialIdAndProvider(socialId, SocialAccount.APPLE)
                .map(existingUser -> {
                    if (email != null && !email.isBlank()) {
                        existingUser.updateEmail(email);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userService.createUser(
                        new UserCreateCommand(
                                socialId,
                                userService.generateUniqueNickname(),
                                SocialAccount.APPLE,
                                randomDefaultProfileImageUrl(),
                                email
                        )
                ));

        return createTokenResponse(user);
    }

    /**
     * Google 소셜 로그인
     * 1. ID Token 검증 및 사용자 정보 추출 (socialId만 사용)
     * 2. 기존 회원이면 로그인, 신규 회원이면 랜덤 닉네임/프로필로 가입 처리
     * 3. JWT Access/Refresh Token 발급
     */
    public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
        // Google ID Token 검증 및 사용자 정보 조회 (socialId 확인용)
        GoogleUserInfo googleUserInfo = googleOAuthService.verifyAndGetUserInfo(request.idToken());

        // 기존 회원 조회 또는 신규 가입 (신규 회원은 랜덤 닉네임/프로필 이미지 부여)
        String socialId = googleUserInfo.getSub();
        String email = googleUserInfo.getEmail();
        User user = userService.findBySocialIdAndProvider(socialId, SocialAccount.GOOGLE)
                .map(existingUser -> {
                    if (email != null && !email.isBlank()) {
                        existingUser.updateEmail(email);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userService.createUser(
                        new UserCreateCommand(
                                socialId,
                                userService.generateUniqueNickname(),
                                SocialAccount.GOOGLE,
                                randomDefaultProfileImageUrl(),
                                email
                        )
                ));

        return createTokenResponse(user);
    }

    /**
     * Access Token 갱신
     * - Refresh Token이 유효하고 타입이 refresh인 경우에만 새 Access Token 발급
     * - DB에 Refresh Token이 존재하는지 추가 검증
     * - Refresh Token은 그대로 유지
     */
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        if (!jwtTokenService.validateRefreshToken(request.refreshToken())) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (refreshTokenService.findByToken(request.refreshToken()).isEmpty()) {
            throw new GlobalException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenService.getUserIdFromToken(request.refreshToken());
        User user = userService.findById(userId);

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        return new TokenResponse(accessToken, request.refreshToken(), "Bearer", UserResponse.from(user));
    }

    private TokenResponse createTokenResponse(User user) {
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        String refreshToken = jwtTokenService.createRefreshToken(user.getId());

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(jwtTokenService.getRefreshTokenExpirationMillis() * 1_000_000);
        refreshTokenService.save(new RefreshTokenCreateCommand(user.getId(), refreshToken, expiresAt));

        return new TokenResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    /**
     * 기본 프로필 이미지 URL 생성
     * - CloudFront 도메인 + "/" + 파일명
     */
    private String randomDefaultProfileImageUrl() {
        // CloudFront 도메인이 설정되지 않은 경우 예외 처리
        if (cloudfrontDomain == null || cloudfrontDomain.isBlank()) {
            throw new GlobalException(AuthErrorCode.CLOUDFRONT_DOMAIN_NOT_CONFIGURED);
        }

        String domain = cloudfrontDomain.trim();

        // 끝에 "/"가 있으면 제거
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        return domain + "/" + DefaultProfileImage.randomFileName();
    }
}
