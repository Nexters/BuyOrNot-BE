package com.nexters.sseotdabwa.api.auth.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.sseotdabwa.api.auth.dto.AppleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.GoogleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.entity.RefreshToken;
import com.nexters.sseotdabwa.domain.auth.repository.RefreshTokenRepository;
import com.nexters.sseotdabwa.domain.auth.service.AppleOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.GoogleOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.auth.service.KakaoOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.external.AppleUserInfo;
import com.nexters.sseotdabwa.domain.auth.service.external.GoogleUserInfo;
import com.nexters.sseotdabwa.domain.auth.service.external.KakaoUserInfo;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @MockBean
    private AppleOAuthService appleOAuthService;

    @MockBean
    private GoogleOAuthService googleOAuthService;

    @Test
    @DisplayName("카카오 로그인 성공 - 신규 사용자")
    void loginWithKakao_newUser_success() throws Exception {
        // given
        long uniqueKakaoId = System.nanoTime();
        KakaoUserInfo mockUserInfo = createMockKakaoUserInfo(uniqueKakaoId, "test@kakao.com", "테스트", null);

        given(kakaoOAuthService.getUserInfo(anyString())).willReturn(mockUserInfo);

        KakaoLoginRequest request = new KakaoLoginRequest("valid_kakao_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.status").value("200"));
    }

    private KakaoUserInfo createMockKakaoUserInfo(Long id, String email, String nickname, String profileImage) {
        KakaoUserInfo userInfo = new KakaoUserInfo();
        ReflectionTestUtils.setField(userInfo, "id", id);

        KakaoUserInfo.KakaoAccount.Profile profile = new KakaoUserInfo.KakaoAccount.Profile();
        ReflectionTestUtils.setField(profile, "nickname", nickname);
        ReflectionTestUtils.setField(profile, "profileImageUrl", profileImage);

        KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount();
        ReflectionTestUtils.setField(account, "email", email);
        ReflectionTestUtils.setField(account, "profile", profile);

        ReflectionTestUtils.setField(userInfo, "kakaoAccount", account);

        return userInfo;
    }

    @Test
    @DisplayName("카카오 로그인 성공 - 기존 사용자")
    void loginWithKakao_existingUser_success() throws Exception {
        // given
        long uniqueKakaoId = System.nanoTime();
        String uniqueSocialId = String.valueOf(uniqueKakaoId);
        String uniqueNickname = "기존사용자_" + UUID.randomUUID().toString().substring(0, 8);
        User existingUser = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.KAKAO)
                .build();
        userRepository.save(existingUser);

        KakaoUserInfo mockUserInfo = createMockKakaoUserInfo(uniqueKakaoId, "test@kakao.com", "새닉네임", null);

        given(kakaoOAuthService.getUserInfo(anyString())).willReturn(mockUserInfo);

        KakaoLoginRequest request = new KakaoLoginRequest("valid_kakao_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("카카오 토큰이 비어있으면 400 에러")
    void loginWithKakao_emptyToken_returns400() throws Exception {
        // given
        KakaoLoginRequest request = new KakaoLoginRequest("");

        // when & then
        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("유효하지 않은 카카오 토큰으로 로그인 시 401 에러")
    void loginWithKakao_invalidToken_returns401() throws Exception {
        // given
        given(kakaoOAuthService.getUserInfo(anyString()))
                .willThrow(new GlobalException(AuthErrorCode.KAKAO_INVALID_TOKEN));

        KakaoLoginRequest request = new KakaoLoginRequest("invalid_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_102"));
    }

    @Test
    @DisplayName("카카오 로그인 시 Refresh Token이 DB에 저장됨")
    void loginWithKakao_savesRefreshTokenToDb() throws Exception {
        // given
        long uniqueKakaoId = System.nanoTime();
        KakaoUserInfo mockUserInfo = createMockKakaoUserInfo(uniqueKakaoId, "test@kakao.com", "테스트", null);

        given(kakaoOAuthService.getUserInfo(anyString())).willReturn(mockUserInfo);

        KakaoLoginRequest request = new KakaoLoginRequest("valid_kakao_token");

        // when
        mockMvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        assertThat(refreshTokenRepository.count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_success() throws Exception {
        // given
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "테스트_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.KAKAO)
                .build();
        User savedUser = userRepository.save(user);

        String validRefreshToken = jwtTokenService.createRefreshToken(savedUser.getId());

        // DB에 Refresh Token 저장 (새 로직에서는 DB 존재 여부도 검증)
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(savedUser.getId())
                .token(validRefreshToken)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        TokenRefreshRequest request = new TokenRefreshRequest(validRefreshToken);

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").value(validRefreshToken));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 401 에러")
    void refreshToken_invalidToken_returns401() throws Exception {
        // given
        TokenRefreshRequest request = new TokenRefreshRequest("invalid_refresh_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
    }

    @Test
    @DisplayName("유효한 JWT이지만 DB에 없는 Refresh Token으로 갱신 시 401 에러")
    void refreshToken_tokenNotInDb_returns401() throws Exception {
        // given
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "테스트_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.KAKAO)
                .build();
        User savedUser = userRepository.save(user);

        // 유효한 JWT이지만 DB에 저장하지 않음
        String validRefreshToken = jwtTokenService.createRefreshToken(savedUser.getId());
        TokenRefreshRequest request = new TokenRefreshRequest(validRefreshToken);

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
    }

    // ==================== Apple 로그인 테스트 ====================

    @Test
    @DisplayName("Apple 로그인 성공 - 신규 사용자 (랜덤 닉네임/프로필 이미지 부여)")
    void loginWithApple_newUser_success() throws Exception {
        // given
        String uniqueAppleSub = "apple_user_" + System.nanoTime();
        AppleUserInfo mockUserInfo = AppleUserInfo.builder()
                .sub(uniqueAppleSub)
                .build();

        given(appleOAuthService.getAppleUserInfo(anyString(), any())).willReturn(mockUserInfo);

        AppleLoginRequest request = new AppleLoginRequest("valid_authorization_code", null);

        // when & then
        mockMvc.perform(post("/api/v1/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("Apple 로그인 성공 - 기존 사용자")
    void loginWithApple_existingUser_success() throws Exception {
        // given
        String uniqueAppleSub = "apple_user_" + System.nanoTime();
        String uniqueNickname = "기존사용자_" + UUID.randomUUID().toString().substring(0, 8);
        User existingUser = User.builder()
                .socialId(uniqueAppleSub)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.APPLE)
                .build();
        userRepository.save(existingUser);

        AppleUserInfo mockUserInfo = AppleUserInfo.builder()
                .sub(uniqueAppleSub)
                .build();

        given(appleOAuthService.getAppleUserInfo(anyString(), any())).willReturn(mockUserInfo);

        AppleLoginRequest request = new AppleLoginRequest("valid_authorization_code", null);

        // when & then
        mockMvc.perform(post("/api/v1/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("Apple Authorization Code가 비어있으면 400 에러")
    void loginWithApple_emptyCode_returns400() throws Exception {
        // given
        AppleLoginRequest request = new AppleLoginRequest("", null);

        // when & then
        mockMvc.perform(post("/api/v1/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("유효하지 않은 Apple Authorization Code로 로그인 시 401 에러")
    void loginWithApple_invalidCode_returns401() throws Exception {
        // given
        given(appleOAuthService.getAppleUserInfo(anyString(), any()))
                .willThrow(new GlobalException(AuthErrorCode.APPLE_INVALID_AUTHORIZATION_CODE));

        AppleLoginRequest request = new AppleLoginRequest("invalid_code", null);

        // when & then
        mockMvc.perform(post("/api/v1/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_207"));
    }

    @Test
    @DisplayName("Apple Token API 호출 실패 시 502 에러")
    void loginWithApple_tokenApiFailed_returns502() throws Exception {
        // given
        given(appleOAuthService.getAppleUserInfo(anyString(), any()))
                .willThrow(new GlobalException(AuthErrorCode.APPLE_TOKEN_API_FAILED));

        AppleLoginRequest request = new AppleLoginRequest("valid_code", null);

        // when & then
        mockMvc.perform(post("/api/v1/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("AUTH_208"));
    }

    // ==================== Google 로그인 테스트 ====================

    @Test
    @DisplayName("Google 로그인 성공 - 신규 사용자 (랜덤 닉네임/프로필 이미지 부여)")
    void loginWithGoogle_newUser_success() throws Exception {
        // given
        String uniqueGoogleSub = "google_user_" + System.nanoTime();
        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .sub(uniqueGoogleSub)
                .build();

        given(googleOAuthService.verifyAndGetUserInfo(anyString())).willReturn(mockUserInfo);

        GoogleLoginRequest request = new GoogleLoginRequest("valid_id_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("Google 로그인 성공 - 기존 사용자")
    void loginWithGoogle_existingUser_success() throws Exception {
        // given
        String uniqueGoogleSub = "google_user_" + System.nanoTime();
        String uniqueNickname = "기존사용자_" + UUID.randomUUID().toString().substring(0, 8);
        User existingUser = User.builder()
                .socialId(uniqueGoogleSub)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.GOOGLE)
                .build();
        userRepository.save(existingUser);

        GoogleUserInfo mockUserInfo = GoogleUserInfo.builder()
                .sub(uniqueGoogleSub)
                .build();

        given(googleOAuthService.verifyAndGetUserInfo(anyString())).willReturn(mockUserInfo);

        GoogleLoginRequest request = new GoogleLoginRequest("valid_id_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("Google ID Token이 비어있으면 400 에러")
    void loginWithGoogle_emptyToken_returns400() throws Exception {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest("");

        // when & then
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("유효하지 않은 Google ID Token으로 로그인 시 401 에러")
    void loginWithGoogle_invalidToken_returns401() throws Exception {
        // given
        given(googleOAuthService.verifyAndGetUserInfo(anyString()))
                .willThrow(new GlobalException(AuthErrorCode.GOOGLE_INVALID_TOKEN));

        GoogleLoginRequest request = new GoogleLoginRequest("invalid_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_301"));
    }

    @Test
    @DisplayName("Google 공개키 조회 실패 시 502 에러")
    void loginWithGoogle_keyFetchFailed_returns502() throws Exception {
        // given
        given(googleOAuthService.verifyAndGetUserInfo(anyString()))
                .willThrow(new GlobalException(AuthErrorCode.GOOGLE_KEY_FETCH_FAILED));

        GoogleLoginRequest request = new GoogleLoginRequest("valid_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("AUTH_305"));
    }
}
