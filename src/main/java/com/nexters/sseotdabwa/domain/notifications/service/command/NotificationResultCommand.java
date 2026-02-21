package com.nexters.sseotdabwa.domain.notifications.service.command;

/**
 * 알림 리스트 카드에 표시할 투표 결과 계산값
 */
public record NotificationResultCommand(
        Integer resultPercent,
        String resultLabel
) {}
