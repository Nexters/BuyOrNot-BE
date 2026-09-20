package com.nexters.sseotdabwa.api.comments.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequestGuest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        String content,
        @NotBlank(message = "닉네임은 필수입니다.")
        String guestNickname,
        @NotBlank(message = "투표 인증 토큰은 필수입니다.")
        String voteToken
) {}
