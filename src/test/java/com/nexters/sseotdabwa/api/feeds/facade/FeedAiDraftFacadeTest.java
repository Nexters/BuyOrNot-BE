package com.nexters.sseotdabwa.api.feeds.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nexters.sseotdabwa.api.feeds.dto.FeedAiDraftRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedAiDraftResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.comments.service.CommentProfanityFilter;
import com.nexters.sseotdabwa.domain.feeds.ai.AiDraftGenerator;
import com.nexters.sseotdabwa.domain.feeds.ai.AiDraftRateLimiter;
import com.nexters.sseotdabwa.domain.feeds.ai.AiFeedDraft;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.users.entity.User;

class FeedAiDraftFacadeTest {

    private static final String CLOUDFRONT = "https://cdn.buy-or-not.com";

    private AiDraftGenerator generator;
    private AiDraftRateLimiter rateLimiter;
    private CommentProfanityFilter profanityFilter;
    private FeedAiDraftFacade facade;
    private User user;

    @BeforeEach
    void setUp() {
        generator = mock(AiDraftGenerator.class);
        rateLimiter = mock(AiDraftRateLimiter.class);
        profanityFilter = mock(CommentProfanityFilter.class);
        AwsProperties awsProperties = new AwsProperties(
                "ap-northeast-2",
                new AwsProperties.S3("buy-or-not", "feeds", 10, 20971520L),
                new AwsProperties.Cloudfront(CLOUDFRONT + "/")
        );
        facade = new FeedAiDraftFacade(generator, rateLimiter, profanityFilter, awsProperties);

        user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(rateLimiter.isExceeded(any())).thenReturn(false);
        when(profanityFilter.containsProfanity(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("초안 생성 성공 - 공백 정리, 사진에서 읽은 가격 반환, 이미지는 CloudFront URL로 전달")
    void createDraft_success() {
        when(generator.generate(anyList()))
                .thenReturn(new AiFeedDraft("  후드집업 이거 어때  ", "포켓 디테일이 귀여운데 비슷한 거 있는 것 같기도 해서 고민되네", FeedCategory.FASHION, 39000L));

        FeedAiDraftResponse response = facade.createDraft(user, request("feeds/abc_hood.jpg", "feeds/def_hood2.png"));

        assertThat(response.title()).isEqualTo("후드집업 이거 어때");
        assertThat(response.content()).isEqualTo("포켓 디테일이 귀여운데 비슷한 거 있는 것 같기도 해서 고민되네");
        assertThat(response.category()).isEqualTo(FeedCategory.FASHION);
        assertThat(response.price()).isEqualTo(39000L);
        assertThat(capturedUrls()).containsExactly(
                CLOUDFRONT + "/feeds/abc_hood.jpg",
                CLOUDFRONT + "/feeds/def_hood2.png");
    }

    @Test
    @DisplayName("가격을 모르면(0) / 음수 / 비현실적으로 큰 값이면 price는 null")
    void createDraft_priceFallbacks() {
        for (Long price : new Long[] {0L, -1000L, FeedAiDraftFacade.MAX_PRICE + 1, null}) {
            when(generator.generate(anyList())).thenReturn(new AiFeedDraft("t", "c", FeedCategory.ETC, price));

            assertThat(facade.createDraft(user, request("feeds/a.jpg")).price()).isNull();
        }
    }

    @Test
    @DisplayName("한글/공백 파일명은 URL 인코딩, 중복 key는 한 번만 전달")
    void createDraft_encodesAndDeduplicatesKeys() {
        when(generator.generate(anyList())).thenReturn(new AiFeedDraft("t", "c", FeedCategory.ETC, null));

        facade.createDraft(user, request("feeds/abc_가습기 사진.jpg", "feeds/abc_가습기 사진.jpg"));

        assertThat(capturedUrls()).containsExactly(
                CLOUDFRONT + "/feeds/abc_%EA%B0%80%EC%8A%B5%EA%B8%B0%20%EC%82%AC%EC%A7%84.jpg");
    }

    @Test
    @DisplayName("제목 40자 / 내용 100자 초과 시 잘라냄 (이모지 서로게이트 쌍은 반으로 자르지 않음)")
    void createDraft_truncates() {
        String longTitle = "가".repeat(39) + "😀" + "나나";
        String longContent = "다".repeat(150);
        when(generator.generate(anyList())).thenReturn(new AiFeedDraft(longTitle, longContent, FeedCategory.BEAUTY, null));

        FeedAiDraftResponse response = facade.createDraft(user, request("feeds/a.jpg"));

        assertThat(response.title()).isEqualTo("가".repeat(39));
        assertThat(response.content()).hasSize(FeedAiDraftFacade.MAX_CONTENT_LENGTH);
    }

    @Test
    @DisplayName("카테고리가 없으면 ETC, 빈 문자열/금칙어가 포함된 필드는 null")
    void createDraft_fallbacks() {
        when(generator.generate(anyList())).thenReturn(new AiFeedDraft("   ", "욕설 포함 문장", null, null));
        when(profanityFilter.containsProfanity("욕설 포함 문장")).thenReturn(true);

        FeedAiDraftResponse response = facade.createDraft(user, request("feeds/a.jpg"));

        assertThat(response.title()).isNull();
        assertThat(response.content()).isNull();
        assertThat(response.category()).isEqualTo(FeedCategory.ETC);
    }

    @Test
    @DisplayName("feeds/ 경로가 아닌 key, 외부 URL, 상위 경로 이동은 FEED_014 (AI 호출 안 함)")
    void createDraft_invalidKey() {
        for (String key : List.of("profiles/a.jpg", "https://evil.com/a.jpg", "feeds/../secret.jpg", "feeds/", "feedsx/a.jpg", "feeds/a.jpg?x=1")) {
            assertThatThrownBy(() -> facade.createDraft(user, request(key)))
                    .isInstanceOf(GlobalException.class)
                    .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_INVALID_IMAGE);
        }
        verify(generator, never()).generate(anyList());
    }

    @Test
    @DisplayName("HEIC는 제외하고 나머지 사진만 전달, 모두 HEIC면 FEED_015")
    void createDraft_heic() {
        when(generator.generate(anyList())).thenReturn(new AiFeedDraft("t", "c", FeedCategory.ETC, null));

        facade.createDraft(user, request("feeds/a.HEIC", "feeds/b.jpg"));
        assertThat(capturedUrls()).containsExactly(CLOUDFRONT + "/feeds/b.jpg");

        assertThatThrownBy(() -> facade.createDraft(user, request("feeds/a.heic", "feeds/b.heif")))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_UNSUPPORTED_IMAGE);
    }

    @Test
    @DisplayName("빈도 제한 초과 시 FEED_016 (AI 호출 안 함)")
    void createDraft_rateLimited() {
        when(rateLimiter.isExceeded(1L)).thenReturn(true);

        assertThatThrownBy(() -> facade.createDraft(user, request("feeds/a.jpg")))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_RATE_LIMIT_EXCEEDED);
        verify(generator, never()).generate(anyList());
    }

    @Test
    @DisplayName("잘못된 key 요청은 빈도 제한 횟수를 소모하지 않음")
    void createDraft_invalidKeyDoesNotConsumeQuota() {
        assertThatThrownBy(() -> facade.createDraft(user, request("profiles/a.jpg")))
                .isInstanceOf(GlobalException.class);
        verify(rateLimiter, never()).isExceeded(any());
    }

    @Test
    @DisplayName("비회원 초안 생성 성공 - 회원과 같은 검증/결과, 빈도 제한은 IP/디바이스 기준")
    void createGuestDraft_success() {
        when(rateLimiter.isExceededForGuest("1.2.3.4", "device-1")).thenReturn(false);
        when(generator.generate(anyList())).thenReturn(new AiFeedDraft(" 후드집업 이거 어때 ", "c", null, 0L));

        FeedAiDraftResponse response = facade.createGuestDraft(request("feeds/a.jpg"), "1.2.3.4", "device-1");

        assertThat(response.title()).isEqualTo("후드집업 이거 어때");
        assertThat(response.category()).isEqualTo(FeedCategory.ETC);
        assertThat(response.price()).isNull();
        verify(rateLimiter).isExceededForGuest("1.2.3.4", "device-1");
        verify(rateLimiter, never()).isExceeded(any());
    }

    @Test
    @DisplayName("비회원 빈도 제한 초과 시 FEED_016, 잘못된 key는 FEED_014 (둘 다 AI 호출 안 함)")
    void createGuestDraft_rejected() {
        when(rateLimiter.isExceededForGuest("1.2.3.4", null)).thenReturn(true);

        assertThatThrownBy(() -> facade.createGuestDraft(request("feeds/a.jpg"), "1.2.3.4", null))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_RATE_LIMIT_EXCEEDED);
        assertThatThrownBy(() -> facade.createGuestDraft(request("https://evil.com/a.jpg"), "5.6.7.8", null))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_INVALID_IMAGE);
        verify(generator, never()).generate(anyList());
    }

    @Test
    @DisplayName("생성기 실패(FEED_017)는 그대로 전파")
    void createDraft_generatorFailure() {
        when(generator.generate(anyList())).thenThrow(new GlobalException(FeedErrorCode.FEED_AI_DRAFT_FAILED));

        assertThatThrownBy(() -> facade.createDraft(user, request("feeds/a.jpg")))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(FeedErrorCode.FEED_AI_DRAFT_FAILED);
    }

    private FeedAiDraftRequest request(String... keys) {
        return new FeedAiDraftRequest(List.of(keys));
    }

    @SuppressWarnings("unchecked")
    private List<String> capturedUrls() {
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(generator, org.mockito.Mockito.atLeastOnce()).generate(captor.capture());
        return captor.getValue();
    }
}
