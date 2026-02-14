package com.nexters.sseotdabwa.domain.feeds.exception;

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

    FEED_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "FEED_001", "내용은 100자 이하로 입력해주세요."),
    FEED_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "FEED_002", "이미지는 필수입니다."),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "FEED_003", "피드를 찾을 수 없습니다."),
    FEED_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "FEED_004", "본인의 피드만 삭제할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
