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
    IMAGE_01("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image1.png"),
    IMAGE_02("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image2.png"),
    IMAGE_03("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image3.png"),
    IMAGE_04("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image4.png"),
    IMAGE_05("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image5.png"),
    IMAGE_06("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image6.png"),
    IMAGE_07("https://sseotdabwa-image.s3.ap-northeast-2.amazonaws.com/Profile_image7.png");

    private final String url;

    private static final DefaultProfileImage[] CACHED_VALUES = values();

    public static DefaultProfileImage random() {
        return CACHED_VALUES[ThreadLocalRandom.current().nextInt(CACHED_VALUES.length)];
    }

    public static String randomUrl() {
        return random().getUrl();
    }
}
