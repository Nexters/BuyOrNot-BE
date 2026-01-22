package com.nexters.sseotdabwa.common.exception;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;

    public static ErrorResponse of(LocalDateTime timestamp, int status, String code, String message, String path) {
        return new ErrorResponse(timestamp, status, code, message, path);
    }

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                path
        );
    }

    public static ErrorResponse of(GlobalException exception, String path) {
        ErrorCode errorCode = exception.getErrorCode();
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                exception.getMessage(),
                path
        );
    }
}
