package com.nexters.sseotdabwa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Google 로그인 요청 DTO
 * - iOS/Android/Web에서 Google SDK로 로그인 후 받은 ID Token 전달
 */
public record GoogleLoginRequest(
        @NotBlank(message = "Google ID Token은 필수입니다.")
        String idToken
) {
}
