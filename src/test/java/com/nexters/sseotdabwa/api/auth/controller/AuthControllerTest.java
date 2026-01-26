package com.nexters.sseotdabwa.api.auth.controller;

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
import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.auth.service.KakaoOAuthService;
import com.nexters.sseotdabwa.domain.auth.service.dto.KakaoUserInfo;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

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

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Test
    @DisplayName("카카오 로그인 성공 - 신규 사용자")
    void loginWithKakao_newUser_success() throws Exception {
        // given
        KakaoUserInfo mockUserInfo = createMockKakaoUserInfo(12345L, "test@kakao.com", "테스트", null);

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
        User existingUser = User.builder()
                .socialId("12345")
                .nickname("기존사용자")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        userRepository.save(existingUser);

        KakaoUserInfo mockUserInfo = createMockKakaoUserInfo(12345L, "test@kakao.com", "새닉네임", null);

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
    @DisplayName("토큰 갱신 성공")
    void refreshToken_success() throws Exception {
        // given
        User user = User.builder()
                .socialId("12345")
                .nickname("테스트")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        User savedUser = userRepository.save(user);

        String validRefreshToken = jwtTokenService.createRefreshToken(savedUser.getId());
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
}
