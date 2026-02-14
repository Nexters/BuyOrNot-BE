package com.nexters.sseotdabwa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청 DTO
 */
public record LogoutRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
