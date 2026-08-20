package com.nexters.sseotdabwa.domain.notifications.enums;

public enum NotificationType {
    MY_FEED_CLOSED,                // 내가 올린 투표 종료
    PARTICIPATED_FEED_CLOSED,      // 내가 참여한 투표 종료
    MY_FEED_VOTED_1,               // 내가 올린 투표 1명 참여
    MY_FEED_VOTED_10,              // 내가 올린 투표 10명 참여
    MARKETING_NO_VOTE,             // 투표 미등록 유저 마케팅 푸시
    MARKETING_INACTIVE_3D,         // 마지막 업로드 후 3일 미활동 재참여 유도
    MARKETING_INACTIVE_7D,         // 마지막 업로드 후 7일 미활동 재참여 유도
    MARKETING_ONBOARDING_INACTIVE  // 가입 후 2일 미업로드 온보딩 유도
}
