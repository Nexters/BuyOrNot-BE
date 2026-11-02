package com.nexters.sseotdabwa.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 app.mail.* 설정 (이메일 알림 발송용).
 * - SMTP 계정/앱 비밀번호는 여기 두지 않고 spring.mail.username/password(환경변수 SPRING_MAIL_USERNAME/PASSWORD)로만 주입한다.
 *
 * @param fromAddress 발신 주소
 * @param fromName    발신자 표시 이름
 * @param webBaseUrl  메일 본문 버튼이 이동할 웹 주소 (예: https://buy-or-not.com)
 * @param apiBaseUrl  수신 거부 링크가 호출할 API 주소 (예: https://api.buy-or-not.com)
 */
@ConfigurationProperties(prefix = "app.mail")
public record EmailNotificationProperties(
        String fromAddress,
        String fromName,
        String webBaseUrl,
        String apiBaseUrl
) {}
