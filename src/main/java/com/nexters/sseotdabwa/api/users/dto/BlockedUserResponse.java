package com.nexters.sseotdabwa.api.users.dto;

import java.time.LocalDateTime;

import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.entity.UserBlock;

public record BlockedUserResponse(
        Long userId,
        String nickname,
        String profileImage,
        LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock userBlock) {
        User blocked = userBlock.getBlockedUser();
        return new BlockedUserResponse(
                blocked.getId(),
                blocked.getNickname(),
                blocked.getProfileImage(),
                userBlock.getCreatedAt()
        );
    }
}
