package com.nexters.sseotdabwa.domain.auth.service.command;

import java.time.LocalDateTime;

public record RefreshTokenCreateCommand(
        Long userId,
        String token,
        LocalDateTime expiresAt
) {
}
