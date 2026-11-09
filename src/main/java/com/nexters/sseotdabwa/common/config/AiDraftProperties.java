package com.nexters.sseotdabwa.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 app.ai.* 설정 (사진 → AI 피드 초안 생성용).
 * - API 키는 yaml에 직접 쓰지 않고 환경변수 ANTHROPIC_API_KEY(k8s Secret buyornot-db)로만 주입한다.
 * - api-key가 비어 있으면 NoopAiDraftGenerator가 동작해 기능만 비활성화되고 서버는 정상 기동한다.
 *
 * @param apiKey    Anthropic API 키
 * @param model     Claude 모델 ID (예: claude-haiku-5-5) — 품질이 부족하면 설정값만 바꿔 교체
 * @param timeout   Claude API 요청 타임아웃
 * @param maxTokens 응답 최대 토큰 수
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiDraftProperties(
        String apiKey,
        String model,
        Duration timeout,
        Long maxTokens
) {
    private static final String DEFAULT_MODEL = "claude-haiku-5-5";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final long DEFAULT_MAX_TOKENS = 1024L;

    public AiDraftProperties {
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (maxTokens == null) {
            maxTokens = DEFAULT_MAX_TOKENS;
        }
    }
}
