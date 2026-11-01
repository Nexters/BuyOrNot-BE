package com.nexters.sseotdabwa.domain.notifications.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 메일 계정(spring.mail.username)이 없는 환경용 — 실제로 보내지 않고 로그만 남긴다
 */
@Slf4j
@Component
@ConditionalOnExpression("'${spring.mail.username:}' == ''")
public class NoopEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.info("[NOOP_EMAIL] subject={}", message.subject());
    }
}
