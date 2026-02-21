package com.nexters.sseotdabwa.api.notifications.exception;

import org.springframework.http.HttpStatus;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI_001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_FEED_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "NOTI_002", "알림과 연결된 피드를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
