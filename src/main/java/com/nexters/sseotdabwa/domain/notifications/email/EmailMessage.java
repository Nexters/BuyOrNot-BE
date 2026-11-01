package com.nexters.sseotdabwa.domain.notifications.email;

/**
 * 발송할 이메일 1통
 *
 * @param unsubscribeUrl 수신 거부 URL — List-Unsubscribe 헤더에 들어가 Gmail 등에서 "구독 취소" 버튼으로 노출된다
 */
public record EmailMessage(
        String to,
        String subject,
        String html,
        String unsubscribeUrl
) {}
