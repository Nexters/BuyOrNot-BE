package com.nexters.sseotdabwa.api.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공감/공감 취소 결과 (FE가 즉시 반영할 수 있도록 최신 상태를 내려줌)")
public record CommentLikeResponse(
        @Schema(description = "현재 공감 수")
        long likeCount,
        @Schema(description = "요청한 회원의 현재 공감 여부")
        boolean isLiked
) {
    public static CommentLikeResponse of(long likeCount, boolean isLiked) {
        return new CommentLikeResponse(likeCount, isLiked);
    }
}
