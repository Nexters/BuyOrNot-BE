package com.nexters.sseotdabwa.api.auth.dto;

import com.nexters.sseotdabwa.api.users.dto.UserResponse;

/**
 * 로그인/토큰 갱신 응답 DTO
 * - accessToken: API 인증에 사용
 * - refreshToken: Access Token 갱신에 사용
 * - tokenType: Bearer - user: 로그인한 사용자 정보
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserResponse user
) {

}
