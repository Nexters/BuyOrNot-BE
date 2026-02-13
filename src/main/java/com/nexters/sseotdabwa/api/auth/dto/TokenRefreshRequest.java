package com.nexters.sseotdabwa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Access Token 갱신 요청 DTO
 */
public record TokenRefreshRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
