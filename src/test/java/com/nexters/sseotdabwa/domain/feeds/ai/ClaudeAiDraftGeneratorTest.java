package com.nexters.sseotdabwa.domain.feeds.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anthropic.models.messages.MessageCreateParams;
import com.nexters.sseotdabwa.common.config.AiDraftProperties;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;

/**
 * 실제 API는 호출하지 않고 요청 파라미터 구성만 검증한다
 */
class ClaudeAiDraftGeneratorTest {

    @Test
    @DisplayName("이미지 URL + 지시문, 설정한 모델/토큰, thinking 비활성, 카테고리 enum이 담긴 JSON 스키마로 요청")
    void buildParams() {
        AiDraftProperties properties = new AiDraftProperties("test-key", null, null, null);
        ClaudeAiDraftGenerator generator = new ClaudeAiDraftGenerator(properties);

        MessageCreateParams params = generator.buildParams(List.of(
                "https://cdn.buy-or-not.com/feeds/a.jpg",
                "https://cdn.buy-or-not.com/feeds/b.png"
        ), AiDraftPrompt.TitleStyle.ASK_OPINION).rawParams();
        String body = params.toString();

        assertThat(params.model().asString()).isEqualTo("claude-haiku-5-5");
        assertThat(params.maxTokens()).isEqualTo(1024L);
        assertThat(body)
                .contains("https://cdn.buy-or-not.com/feeds/a.jpg")
                .contains("https://cdn.buy-or-not.com/feeds/b.png")
                .contains(AiDraftPrompt.INSTRUCTION)
                .contains("[제목 스타일] 의견을 묻는 제목")
                .contains("disabled");
        // 시스템 프롬프트에도 카테고리 이름이 있으므로, 응답 스키마(output_config)만 따로 검사
        String schema = params.outputConfig().orElseThrow().toString();
        String categoryEnum = Arrays.stream(FeedCategory.values()).map(Enum::name).collect(Collectors.joining(", "));
        assertThat(schema)
                .contains("enum=[" + categoryEnum + "]")
                .contains("price={type=integer")
                .contains("required=[category, content, price, title]")
                .contains("additionalProperties=false");
        generator.close();
    }

    @Test
    @DisplayName("시스템 프롬프트에 모든 카테고리와 표시 이름이 들어감")
    void systemPromptContainsCategories() {
        for (FeedCategory category : FeedCategory.values()) {
            assertThat(AiDraftPrompt.SYSTEM).contains("- " + category.name() + ": " + category.getDisplayName());
        }
    }

    @Test
    @DisplayName("제목 스타일은 요청마다 무작위로 골라지고, 모든 스타일이 나올 수 있음")
    void titleStyleIsRandom() {
        Set<AiDraftPrompt.TitleStyle> picked = EnumSet.noneOf(AiDraftPrompt.TitleStyle.class);
        for (int i = 0; i < 500; i++) {
            picked.add(AiDraftPrompt.TitleStyle.random());
        }
        assertThat(picked).containsExactlyInAnyOrder(AiDraftPrompt.TitleStyle.values());
    }
}
