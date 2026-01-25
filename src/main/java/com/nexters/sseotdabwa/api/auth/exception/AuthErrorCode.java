package com.nexters.sseotdabwa.api.auth.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 리프레시 토큰입니다."),

    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_101", "카카오 API 호출에 실패했습니다."),
    KAKAO_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_102", "유효하지 않은 카카오 액세스 토큰입니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_103", "카카오 사용자 정보를 가져오는데 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
