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
    FEED_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "FEED_004", "본인의 피드만 삭제할 수 있습니다."),
    FEED_ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "FEED_005", "이미 신고된 피드입니다."),
    FEED_SELF_REPORT(HttpStatus.BAD_REQUEST, "FEED_006", "본인의 피드는 신고할 수 없습니다."),
    FEED_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "FEED_007", "이미지는 최대 3장까지 업로드할 수 있습니다."),
    FEED_INVALID_LINK(HttpStatus.BAD_REQUEST, "FEED_008", "유효하지 않은 URL입니다. http 또는 https로 시작하는 URL을 입력해주세요."),
    FEED_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "FEED_009", "제목은 40자 이하로 입력해주세요."),
    FEED_IMAGE_INVALID_SIZE(HttpStatus.BAD_REQUEST, "FEED_010", "이미지 크기(width/height)는 1 이상이어야 합니다."),
    FEED_GUEST_PASSWORD_MISMATCH(HttpStatus.FORBIDDEN, "FEED_011", "비밀번호가 일치하지 않습니다."),
    FEED_NOT_GUEST_POST(HttpStatus.FORBIDDEN, "FEED_012", "비회원이 작성한 피드가 아닙니다."),
    FEED_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "FEED_013", "유효하지 않은 커서입니다."),

    // AI 피드 초안
    FEED_AI_DRAFT_INVALID_IMAGE(HttpStatus.BAD_REQUEST, "FEED_014", "유효하지 않은 이미지입니다."),
    FEED_AI_DRAFT_UNSUPPORTED_IMAGE(HttpStatus.BAD_REQUEST, "FEED_015", "AI 초안은 JPG, PNG, WEBP, GIF 사진만 지원해요."),
    FEED_AI_DRAFT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "FEED_016", "잠시 후 다시 시도해주세요."),
    FEED_AI_DRAFT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "FEED_017", "AI 초안을 만들지 못했어요. 직접 입력해주세요."),
    FEED_AI_DRAFT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "FEED_018", "지금은 AI 초안 기능을 사용할 수 없어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
