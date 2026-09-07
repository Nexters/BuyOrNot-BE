package com.nexters.sseotdabwa.api.feeds.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedGuestDeleteRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
