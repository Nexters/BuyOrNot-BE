package com.nexters.sseotdabwa.domain.notifications.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.notifications.entity.PushLog;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.repository.PushLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 푸시 발송 로그 기록
 * - Notification(알림함 콘텐츠)과 무관, 순수 발송 감사(audit) 목적
 * - best-effort: 로그 저장이 실패해도 호출자에게 예외를 전파하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushLogService {

    private final PushLogRepository pushLogRepository;

    @Transactional
    public void record(Long userId, Long feedId, NotificationType type, String title, String body,
                        boolean success, String errorMessage) {
        try {
            PushLog pushLog = PushLog.builder()
                    .userId(userId)
                    .feedId(feedId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .success(success)
                    .errorMessage(errorMessage)
                    .build();

            pushLogRepository.save(pushLog);
        } catch (Exception e) {
            log.warn("푸시 발송 로그 저장 실패. userId={}, feedId={}, type={}", userId, feedId, type, e);
        }
    }
}
