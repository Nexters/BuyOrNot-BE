package com.nexters.sseotdabwa.api.users.dto;

import com.nexters.sseotdabwa.domain.users.entity.User;

public record UserWithdrawResponse(String nickname) {

    public static UserWithdrawResponse from(User user) {
        return new UserWithdrawResponse(user.getNickname());
    }
}
