package com.nexters.sseotdabwa.domain.notifications.email;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.nexters.sseotdabwa.common.config.EmailNotificationProperties;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/**
 * Gmail SMTP 기반 메일 발송 (spring.mail.* 설정 사용)
 * - 계정/앱 비밀번호는 환경변수 SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD (k8s Secret)로만 주입
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${spring.mail.username:}' != ''")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final EmailNotificationProperties properties;

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.html(), true);
            if (message.unsubscribeUrl() != null) {
                // RFC 8058 원클릭 수신 거부 — Gmail이 메일 상단에 "구독 취소" 버튼을 띄운다 (POST로 호출됨)
                mime.addHeader("List-Unsubscribe", "<" + message.unsubscribeUrl() + ">");
                mime.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
            }
            mailSender.send(mime);
        } catch (Exception e) {
            throw new IllegalStateException("이메일 발송 실패", e);
        }
    }
}
