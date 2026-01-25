package com.nexters.sseotdabwa.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.dto.KakaoUserInfo;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KakaoOAuthServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private KakaoOAuthService kakaoOAuthService;

    @Test
    @DisplayName("카카오 사용자 정보 조회 성공")
    void getUserInfo_success() {
        // given
        ReflectionTestUtils.setField(kakaoOAuthService, "userInfoUrl", "https://kapi.kakao.com/v2/user/me");

        KakaoUserInfo mockUserInfo = new KakaoUserInfo();
        ReflectionTestUtils.setField(mockUserInfo, "id", 12345L);

        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(KakaoUserInfo.class)).willReturn(Mono.just(mockUserInfo));

        // when
        KakaoUserInfo result = kakaoOAuthService.getUserInfo("valid_token");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("유효하지 않은 카카오 토큰으로 조회 시 예외 발생")
    void getUserInfo_invalidToken_throwsException() {
        // given
        ReflectionTestUtils.setField(kakaoOAuthService, "userInfoUrl", "https://kapi.kakao.com/v2/user/me");

        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(KakaoUserInfo.class))
                .willReturn(Mono.error(new GlobalException(AuthErrorCode.KAKAO_INVALID_TOKEN)));

        // when & then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo("invalid_token"))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.KAKAO_INVALID_TOKEN);
    }

    @Test
    @DisplayName("카카오 사용자 정보가 없으면 예외 발생")
    void getUserInfo_noUserInfo_throwsException() {
        // given
        ReflectionTestUtils.setField(kakaoOAuthService, "userInfoUrl", "https://kapi.kakao.com/v2/user/me");

        given(webClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(KakaoUserInfo.class)).willReturn(Mono.empty());

        // when & then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo("valid_token"))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.KAKAO_USER_INFO_FAILED);
    }
}
