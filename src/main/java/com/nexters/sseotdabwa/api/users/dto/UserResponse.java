package com.nexters.sseotdabwa.api.users.dto;

import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

/**
 * 사용자 정보 응답 DTO
 */
public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        SocialAccount socialAccount
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getSocialAccount()
        );
    }
}
