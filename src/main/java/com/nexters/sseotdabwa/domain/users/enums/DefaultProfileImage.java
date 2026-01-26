package com.nexters.sseotdabwa.domain.users.enums;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 기본 프로필 이미지 목록
 * - 신규 회원 가입 시 랜덤으로 부여
 * - TODO: 실제 이미지 URL로 교체 필요
 */
@Getter
@RequiredArgsConstructor
public enum DefaultProfileImage {
    IMAGE_01("https://example.com/profiles/default_01.png"),
    IMAGE_02("https://example.com/profiles/default_02.png"),
    IMAGE_03("https://example.com/profiles/default_03.png"),
    IMAGE_04("https://example.com/profiles/default_04.png"),
    IMAGE_05("https://example.com/profiles/default_05.png"),
    IMAGE_06("https://example.com/profiles/default_06.png"),
    IMAGE_07("https://example.com/profiles/default_07.png");

    private final String url;

    private static final DefaultProfileImage[] CACHED_VALUES = values();

    public static DefaultProfileImage random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }

    public static String randomUrl() {
        return random().getUrl();
    }
}
