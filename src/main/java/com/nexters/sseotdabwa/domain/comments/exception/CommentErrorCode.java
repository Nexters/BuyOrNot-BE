package com.nexters.sseotdabwa.domain.comments.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

/**
 * 댓글 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "COMMENT_001", "댓글 내용을 입력해주세요."),
    COMMENT_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "COMMENT_002", "댓글은 100자 이하로 입력해주세요."),
    COMMENT_FEED_CLOSED(HttpStatus.BAD_REQUEST, "COMMENT_003", "마감된 피드에는 댓글을 작성할 수 없습니다."),
    COMMENT_NOT_VOTED(HttpStatus.FORBIDDEN, "COMMENT_004", "투표한 피드에만 댓글을 작성할 수 있습니다."),
    COMMENT_INVALID_VOTE_TOKEN(HttpStatus.FORBIDDEN, "COMMENT_005", "유효하지 않거나 만료된 투표 인증입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
