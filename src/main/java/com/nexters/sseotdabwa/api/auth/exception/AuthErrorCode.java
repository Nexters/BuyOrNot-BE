package com.nexters.sseotdabwa.api.auth.exception;

import org.springframework.http.HttpStatus;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_000", "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 리프레시 토큰입니다."),

    // Kakao OAuth 에러 (AUTH_101 ~ AUTH_104)
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_101", "카카오 API 호출에 실패했습니다."),
    KAKAO_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_102", "유효하지 않은 카카오 액세스 토큰입니다."),
    KAKAO_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_103", "카카오 사용자 정보를 가져오는데 실패했습니다."),
    KAKAO_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AUTH_104", "카카오 API 호출 시간이 초과되었습니다."),

    // Google OAuth 에러 (AUTH_301 ~ AUTH_310)
    GOOGLE_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_301", "유효하지 않은 Google ID Token입니다."),
    GOOGLE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_302", "Google ID Token이 만료되었습니다."),
    GOOGLE_INVALID_ISSUER(HttpStatus.UNAUTHORIZED, "AUTH_303", "Google ID Token의 발급자가 올바르지 않습니다."),
    GOOGLE_INVALID_AUDIENCE(HttpStatus.UNAUTHORIZED, "AUTH_304", "Google ID Token의 대상이 올바르지 않습니다."),
    GOOGLE_KEY_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_305", "Google 공개키를 가져오는데 실패했습니다."),
    GOOGLE_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AUTH_306", "Google API 호출 시간이 초과되었습니다."),

    // Apple OAuth 에러 (AUTH_201 ~ AUTH_210)
    APPLE_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_201", "유효하지 않은 Apple Identity Token입니다."),
    APPLE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_202", "Apple Identity Token이 만료되었습니다."),
    APPLE_INVALID_ISSUER(HttpStatus.UNAUTHORIZED, "AUTH_203", "Apple Identity Token의 발급자가 올바르지 않습니다."),
    APPLE_INVALID_AUDIENCE(HttpStatus.UNAUTHORIZED, "AUTH_204", "Apple Identity Token의 대상이 올바르지 않습니다."),
    APPLE_KEY_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_205", "Apple 공개키를 가져오는데 실패했습니다."),
    APPLE_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AUTH_206", "Apple API 호출 시간이 초과되었습니다."),
    APPLE_INVALID_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH_207", "유효하지 않은 Apple Authorization Code입니다."),
    APPLE_TOKEN_API_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_208", "Apple Token API 호출에 실패했습니다."),
    APPLE_CLIENT_SECRET_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_209", "Apple client_secret 생성에 실패했습니다."),
    APPLE_INVALID_PRIVATE_KEY(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_210", "Apple Private Key가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
