package com.nexters.sseotdabwa.api.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "게스트 댓글 삭제 요청")
public record CommentGuestDeleteRequest(
        @Schema(description = "댓글 작성 시 입력한 비밀번호. 일치해야 삭제된다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
