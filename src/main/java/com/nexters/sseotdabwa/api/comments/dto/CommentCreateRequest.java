package com.nexters.sseotdabwa.api.comments.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content
) {}
