package com.nexters.sseotdabwa.api.feeds.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 피드 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {

    FEED_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "FEED_002", "내용은 100자 이하로 입력해주세요."),
    FEED_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "FEED_003", "이미지는 필수입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
