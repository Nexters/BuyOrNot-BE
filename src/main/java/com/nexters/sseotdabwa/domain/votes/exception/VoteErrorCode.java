package com.nexters.sseotdabwa.domain.votes.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VoteErrorCode implements ErrorCode {

    VOTE_ALREADY_VOTED(HttpStatus.BAD_REQUEST, "VOTE_001", "이미 투표한 피드입니다."),
    VOTE_FEED_CLOSED(HttpStatus.BAD_REQUEST, "VOTE_002", "마감된 피드에는 투표할 수 없습니다."),
    VOTE_OWN_FEED(HttpStatus.BAD_REQUEST, "VOTE_003", "본인의 피드에는 투표할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
