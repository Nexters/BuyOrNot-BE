package com.nexters.sseotdabwa.domain.notifications.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림 도메인 서비스
 * - 알림 조회/읽음처리 등 Notification 핵심 로직 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
}
