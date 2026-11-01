package com.nexters.sseotdabwa.domain.notifications.email;

/**
 * 이메일 발송 추상화
 * - 현재 구현은 Gmail SMTP(SmtpEmailSender). 발송량이 늘면 SES 등으로 구현체만 교체한다.
 * - 메일 계정이 설정되지 않은 환경(local/test 등)에서는 NoopEmailSender가 동작한다.
 * - 발송 실패 시 예외를 던진다 (호출하는 쪽에서 발송 실패 로그를 남기도록)
 */
public interface EmailSender {

    void send(EmailMessage message);
}
