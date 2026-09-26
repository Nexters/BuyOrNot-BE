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
    BLOCK_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_005", "차단 관계가 존재하지 않습니다."),

    // --- Nickname / Profile
    NICKNAME_REQUIRED(HttpStatus.FORBIDDEN, "USER_006", "닉네임을 먼저 설정해주세요."),
    NICKNAME_SPECIAL_CHARACTER(HttpStatus.BAD_REQUEST, "USER_007", "특수문자는 사용할 수 없어요."),
    NICKNAME_WHITESPACE(HttpStatus.BAD_REQUEST, "USER_008", "띄어쓰기는 사용할 수 없어요."),
    NICKNAME_LENGTH_INVALID(HttpStatus.BAD_REQUEST, "USER_009", "최소 3자 이상 입력해주세요."),
    NICKNAME_INVALID_COMPOSITION(HttpStatus.BAD_REQUEST, "USER_010", "한글, 영문, 숫자를 조합해 입력해주세요."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "USER_011", "이미 사용 중인 닉네임이에요."),
    NICKNAME_FORBIDDEN_WORD(HttpStatus.BAD_REQUEST, "USER_012", "사용할 수 없는 닉네임이에요."),
    NICKNAME_CHANGE_COOLDOWN(HttpStatus.BAD_REQUEST, "USER_013", "닉네임은 20일마다 한 번만 변경할 수 있어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
