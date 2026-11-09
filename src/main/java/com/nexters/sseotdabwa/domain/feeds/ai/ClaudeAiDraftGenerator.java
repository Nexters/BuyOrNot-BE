package com.nexters.sseotdabwa.domain.feeds.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.anthropic.models.messages.UrlImageSource;
import com.nexters.sseotdabwa.common.config.AiDraftProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Claude API(비전) 기반 피드 초안 생성
 * - 이미지는 CloudFront 공개 URL을 그대로 넘긴다 (서버가 이미지를 내려받지 않음)
 * - 응답은 구조화 출력(AiFeedDraft JSON 스키마)으로 받아 타입 그대로 파싱
 * - 짧은 분류/작문 작업이라 thinking은 끄고(지연/비용 절감) 재시도는 1회로 제한
 * - API 키는 환경변수 ANTHROPIC_API_KEY (k8s Secret)로만 주입
 */
@Slf4j
@Component
@ConditionalOnExpression("'${app.ai.api-key:}' != ''")
public class ClaudeAiDraftGenerator implements AiDraftGenerator {

    private static final int MAX_RETRIES = 1;

    private final AiDraftProperties properties;
    private final AnthropicClient client;

    public ClaudeAiDraftGenerator(AiDraftProperties properties) {
        this.properties = properties;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(properties.timeout())
                .maxRetries(MAX_RETRIES)
                .build();
    }

    @Override
    public AiFeedDraft generate(List<String> imageUrls) {
        AiDraftPrompt.TitleStyle titleStyle = AiDraftPrompt.TitleStyle.random();
        StructuredMessageCreateParams<AiFeedDraft> params = buildParams(imageUrls, titleStyle);

        long startedAt = System.currentTimeMillis();
        try {
            StructuredMessage<AiFeedDraft> response = client.messages().create(params);
            StopReason stopReason = response.stopReason().orElse(null);
            log.info("[AI_DRAFT] model={} images={} titleStyle={} inputTokens={} outputTokens={} stopReason={} latencyMs={}",
                    properties.model(), imageUrls.size(), titleStyle,
                    response.usage().inputTokens(), response.usage().outputTokens(),
                    stopReason, System.currentTimeMillis() - startedAt);

            // refusal(안전 분류기 거절) / max_tokens(잘린 JSON) 등은 초안으로 쓸 수 없음
            if (!StopReason.END_TURN.equals(stopReason)) {
                throw new GlobalException(FeedErrorCode.FEED_AI_DRAFT_FAILED);
            }
            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(StructuredTextBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new GlobalException(FeedErrorCode.FEED_AI_DRAFT_FAILED));
        } catch (AnthropicException e) {
            // API 오류(이미지 URL 접근 실패, 크레딧 소진, 과부하 등) / 타임아웃 / 응답 파싱 실패
            log.warn("[AI_DRAFT] Claude API 호출 실패. images={} latencyMs={} error={}",
                    imageUrls.size(), System.currentTimeMillis() - startedAt, e.getMessage());
            throw new GlobalException(FeedErrorCode.FEED_AI_DRAFT_FAILED);
        }
    }

    StructuredMessageCreateParams<AiFeedDraft> buildParams(List<String> imageUrls, AiDraftPrompt.TitleStyle titleStyle) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        for (String url : imageUrls) {
            blocks.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
                    .source(UrlImageSource.builder().url(url).build())
                    .build()));
        }
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(AiDraftPrompt.instruction(titleStyle)).build()));

        return MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                .system(AiDraftPrompt.SYSTEM)
                .thinking(ThinkingConfigDisabled.builder().build())
                .addUserMessageOfBlockParams(blocks)
                .outputConfig(AiFeedDraft.class)
                .build();
    }

    @PreDestroy
    public void close() {
        client.close();
    }
}
