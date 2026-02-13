package com.nexters.sseotdabwa.domain.users.service.command;

import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

/**
 * 사용자 생성 커맨드
 * - 소셜 로그인 시 신규 사용자 생성에 사용
 */
public record UserCreateCommand(
        String socialId,
        String nickname,
        SocialAccount socialAccount,
        String profileImage,
        String email
) {
}
