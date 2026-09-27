package com.nexters.sseotdabwa.api.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 댓글 작성 요청")
public record CommentCreateRequest(
        @Schema(description = "댓글 내용 (공백 제거 후 1~300자)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content
) {}
