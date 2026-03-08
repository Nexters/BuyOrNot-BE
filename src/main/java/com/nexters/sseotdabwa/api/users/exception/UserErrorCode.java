package com.nexters.sseotdabwa.api.users.exception;

import org.springframework.http.HttpStatus;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_002", "닉네임 생성에 실패했습니다."),

    // --- Block
    BLOCK_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USER_003", "자기 자신은 차단할 수 없습니다."),
    ALREADY_BLOCKED_USER(HttpStatus.CONFLICT, "USER_004", "이미 차단한 사용자입니다."),
    BLOCK_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_005", "차단 관계가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
