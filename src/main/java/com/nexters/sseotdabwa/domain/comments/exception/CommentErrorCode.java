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
    COMMENT_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "COMMENT_002", "댓글은 300자 이하로 입력해주세요."),
    COMMENT_FEED_CLOSED(HttpStatus.BAD_REQUEST, "COMMENT_003", "마감된 피드에는 댓글을 작성할 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_004", "댓글을 찾을 수 없습니다."),
    COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_005", "본인의 댓글만 삭제할 수 있습니다."),
    COMMENT_NOT_GUEST_COMMENT(HttpStatus.FORBIDDEN, "COMMENT_006", "비회원이 작성한 댓글이 아닙니다."),
    COMMENT_GUEST_PASSWORD_MISMATCH(HttpStatus.FORBIDDEN, "COMMENT_007", "비밀번호가 일치하지 않습니다."),
    COMMENT_SELF_REPORT(HttpStatus.BAD_REQUEST, "COMMENT_008", "본인의 댓글은 신고할 수 없습니다."),
    COMMENT_ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "COMMENT_009", "이미 신고된 댓글입니다."),
    COMMENT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "COMMENT_010", "잠시 후 다시 댓글을 남길 수 있어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
