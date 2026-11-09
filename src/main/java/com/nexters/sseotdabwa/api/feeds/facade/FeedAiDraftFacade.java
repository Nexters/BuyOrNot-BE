package com.nexters.sseotdabwa.api.feeds.facade;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

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

import lombok.RequiredArgsConstructor;

/**
 * 사진 → AI 피드 초안 생성 흐름
 * - 초안은 저장하지 않는다 (사용자가 수정 후 기존 피드 등록 API로 등록)
 * - 이미지 key 검증 → 빈도 제한 → 생성 → 결과 검증(길이/카테고리/금칙어/가격) 순
 * - DB를 쓰지 않으므로 트랜잭션 없음 (외부 API 호출 동안 커넥션을 잡지 않도록)
 */
@Component
@RequiredArgsConstructor
public class FeedAiDraftFacade {

    static final int MAX_TITLE_LENGTH = 40;
    static final int MAX_CONTENT_LENGTH = 100;
    static final long MAX_PRICE = 1_000_000_000L;

    /** Claude가 지원하지 않는 포맷 (업로드는 허용되지만 AI 초안 대상에서는 제외) */
    private static final Set<String> UNSUPPORTED_EXTENSIONS = Set.of("heic", "heif");

    private final AiDraftGenerator aiDraftGenerator;
    private final AiDraftRateLimiter aiDraftRateLimiter;
    private final CommentProfanityFilter profanityFilter;
    private final AwsProperties awsProperties;

    public FeedAiDraftResponse createDraft(User user, FeedAiDraftRequest request) {
        List<String> imageUrls = toImageUrls(request.s3ObjectKeys());
        validateRateLimit(aiDraftRateLimiter.isExceeded(user.getId()));
        return generate(imageUrls);
    }

    /**
     * 비회원 초안 생성 — 회원과 같은 흐름, 빈도 제한만 IP/디바이스ID 기준
     */
    public FeedAiDraftResponse createGuestDraft(FeedAiDraftRequest request, String ip, String deviceId) {
        List<String> imageUrls = toImageUrls(request.s3ObjectKeys());
        validateRateLimit(aiDraftRateLimiter.isExceededForGuest(ip, deviceId));
        return generate(imageUrls);
    }

    private void validateRateLimit(boolean exceeded) {
        if (exceeded) {
            throw new GlobalException(FeedErrorCode.FEED_AI_DRAFT_RATE_LIMIT_EXCEEDED);
        }
    }

    private FeedAiDraftResponse generate(List<String> imageUrls) {
        AiFeedDraft draft = aiDraftGenerator.generate(imageUrls);
        return new FeedAiDraftResponse(
                sanitize(draft.title(), MAX_TITLE_LENGTH),
                sanitize(draft.content(), MAX_CONTENT_LENGTH),
                draft.category() == null ? FeedCategory.ETC : draft.category(),
                sanitizePrice(draft.price())
        );
    }

    /**
     * 모델은 가격을 모르면 0을 낸다 (구조화 출력이 null을 허용하지 않음) → null.
     * 등록 API의 @Positive와 맞추고, 전화번호/모델번호 등을 잘못 읽은 비현실적인 값도 null.
     */
    private Long sanitizePrice(Long price) {
        if (price == null || price <= 0 || price > MAX_PRICE) {
            return null;
        }
        return price;
    }

    /**
     * 우리 버킷의 피드 이미지 경로({keyPrefix}/...)만 허용해 외부 URL/다른 경로 주입을 막고,
     * HEIC처럼 모델이 읽지 못하는 포맷은 제외한 뒤 CloudFront 공개 URL로 바꾼다.
     */
    private List<String> toImageUrls(List<String> s3ObjectKeys) {
        String prefix = awsProperties.s3().keyPrefix().replaceAll("^/+", "").replaceAll("/+$", "") + "/";
        String domain = awsProperties.cloudfront().domain().replaceAll("/$", "");

        List<String> keys = s3ObjectKeys.stream().map(String::trim).distinct().toList();
        for (String key : keys) {
            if (!key.startsWith(prefix) || key.length() == prefix.length()
                    || key.contains("..") || key.contains("://") || key.contains("?") || key.contains("#")) {
                throw new GlobalException(FeedErrorCode.FEED_AI_DRAFT_INVALID_IMAGE);
            }
        }

        List<String> supportedKeys = keys.stream()
                .filter(key -> !UNSUPPORTED_EXTENSIONS.contains(extensionOf(key)))
                .toList();
        if (supportedKeys.isEmpty()) {
            throw new GlobalException(FeedErrorCode.FEED_AI_DRAFT_UNSUPPORTED_IMAGE);
        }

        // 파일명에 한글/특수문자가 들어갈 수 있어 경로를 인코딩 ("/"는 유지)
        return supportedKeys.stream()
                .map(key -> domain + "/" + UriUtils.encodePath(key, StandardCharsets.UTF_8))
                .toList();
    }

    private String extensionOf(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "" : key.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 공백 정리 + 길이 자르기, 금칙어가 있거나 비어 있으면 null (사용자가 직접 입력)
     */
    private String sanitize(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String normalized = text.strip();
        if (normalized.isEmpty() || profanityFilter.containsProfanity(normalized)) {
            return null;
        }
        return truncate(normalized, maxLength);
    }

    /**
     * 등록 API의 @Size 검증(UTF-16 길이)과 같은 기준으로 자르되, 이모지 등 서로게이트 쌍이 반으로 잘리지 않게 한다
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        int end = maxLength;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end).strip();
    }
}
