package com.nexters.sseotdabwa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 로그인 요청 DTO
 * - 클라이언트에서 카카오 SDK로 로그인 후 받은 Access Token 전달
 */
public record KakaoLoginRequest(
        @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
        String accessToken
) {
}
