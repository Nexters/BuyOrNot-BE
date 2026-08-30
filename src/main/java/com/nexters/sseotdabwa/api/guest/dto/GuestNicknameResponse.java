package com.nexters.sseotdabwa.api.guest.dto;

public record GuestNicknameResponse(
        String nickname
) {
    public static GuestNicknameResponse of(String nickname) {
        return new GuestNicknameResponse(nickname);
    }
}
