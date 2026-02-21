package com.nexters.sseotdabwa.domain.prelaunch.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PreLaunchErrorCode implements ErrorCode {

    PRELAUNCH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "PRELAUNCH_001", "이미 등록된 이메일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
