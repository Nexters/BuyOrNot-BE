package com.nexters.sseotdabwa.api.users.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * FCM 토큰 등록 요청 DTO
 * - 클라이언트에서 Firebase SDK로 발급받은 FCM Token을 전달
 * - 로그인 이후(유저 식별 가능) 또는 토큰 갱신 시 서버에 저장하기 위해 사용
 */
public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String fcmToken
) {}
