package com.nexters.sseotdabwa.api.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "답글 페이지 상단 고정용 원댓글 + 헤더 카운트")
public record CommentThreadResponse(
        @Schema(description = "원댓글 (replies는 빈 배열, 답글 목록은 .../replies로 조회)")
        CommentResponse comment,
        @Schema(description = "헤더 \"댓글 (n)\"의 n = 원댓글 1(삭제된 원댓글이면 0) + 답글 수(신고된 답글 제외). 99 초과 표기는 FE")
        long totalCount
) {
    public static CommentThreadResponse of(CommentResponse comment) {
        long rootCount = comment.isDeleted() ? 0L : 1L;
        return new CommentThreadResponse(comment, rootCount + comment.replyCount());
    }
}
