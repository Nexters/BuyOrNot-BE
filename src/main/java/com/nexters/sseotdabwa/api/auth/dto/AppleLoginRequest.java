package com.nexters.sseotdabwa.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Apple 로그인 요청 DTO
 * - iOS/Web에서 Apple SDK로 로그인 후 받은 Authorization Code 전달
 */
public record AppleLoginRequest(
        @NotBlank(message = "Apple Authorization Code는 필수입니다.")
        String authorizationCode
) {
}
