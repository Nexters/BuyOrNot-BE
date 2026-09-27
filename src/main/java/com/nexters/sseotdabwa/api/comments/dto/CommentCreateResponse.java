package com.nexters.sseotdabwa.api.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성 응답")
public record CommentCreateResponse(
        @Schema(description = "생성된 댓글 id")
        Long id
) {
    public static CommentCreateResponse of(Long id) {
        return new CommentCreateResponse(id);
    }
}
