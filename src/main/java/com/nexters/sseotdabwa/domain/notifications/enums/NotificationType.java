package com.nexters.sseotdabwa.domain.notifications.enums;

public enum NotificationType {
    MY_FEED_CLOSED,            // 내가 올린 투표 종료
    PARTICIPATED_FEED_CLOSED,  // 내가 참여한 투표 종료
    MY_FEED_VOTED_1,           // 내가 올린 투표 1명 참여
    MY_FEED_VOTED_10,          // 내가 올린 투표 10명 참여
    MARKETING_NO_VOTE          // 투표 미등록 유저 마케팅 푸시
}
