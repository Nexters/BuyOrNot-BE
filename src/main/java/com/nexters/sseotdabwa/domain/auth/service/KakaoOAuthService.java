package com.nexters.sseotdabwa.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.dto.KakaoUserInfo;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * 카카오 OAuth 서비스
 * - 카카오 API를 호출하여 사용자 정보 조회
 * - 클라이언트에서 받은 카카오 Access Token으로 사용자 정보 요청
 */
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final WebClient webClient;

    @Value("${oauth.kakao.user-info-url}")
    private String userInfoUrl;

    /**
     * 카카오 Access Token으로 사용자 정보 조회
     * @param accessToken 클라이언트에서 카카오 로그인 후 받은 Access Token
     * @return 카카오 사용자 정보 (id, email, nickname, profile_image)
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        return webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new GlobalException(AuthErrorCode.KAKAO_INVALID_TOKEN)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new GlobalException(AuthErrorCode.KAKAO_API_ERROR)))
                .bodyToMono(KakaoUserInfo.class)
                .blockOptional()
                .orElseThrow(() -> new GlobalException(AuthErrorCode.KAKAO_USER_INFO_FAILED));
    }
}
