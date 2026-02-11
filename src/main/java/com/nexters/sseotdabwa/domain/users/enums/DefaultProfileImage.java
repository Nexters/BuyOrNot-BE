package com.nexters.sseotdabwa.domain.users.enums;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 기본 프로필 이미지 목록
 * - 신규 회원 가입 시 랜덤으로 부여
 */
@Getter
@RequiredArgsConstructor
public enum DefaultProfileImage {
    IMAGE_01("Profile_image1.png"),
    IMAGE_02("Profile_image2.png"),
    IMAGE_03("Profile_image3.png"),
    IMAGE_04("Profile_image4.png"),
    IMAGE_05("Profile_image5.png"),
    IMAGE_06("Profile_image6.png"),
    IMAGE_07("Profile_image7.png");

    private final String fileName;

    private static final DefaultProfileImage[] CACHED_VALUES = values();

    public static DefaultProfileImage random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }

    public static String randomFileName() {
        return random().getFileName();
    }
}
