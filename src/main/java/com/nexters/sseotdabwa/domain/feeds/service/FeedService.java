package com.nexters.sseotdabwa.domain.feeds.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;

import lombok.RequiredArgsConstructor;

/**
 * 피드 도메인 서비스
 * - 피드 생성 관련 핵심 비즈니스 로직 처리
 *
 * 정책:
 * - content: 100자 이하
 * - image: presigned 업로드 후 s3ObjectKey 필수
 * - Feed : FeedImage = 1 : 1
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int MAX_CONTENT_LENGTH = 100;

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;

    /**
     * 피드 생성
     */
    @Transactional
    public Long createFeed(FeedCreateCommand command) {
        validate(command);

        Feed feed = Feed.builder()
                .user(command.user())
                .content(normalizeContent(command.content()))
                .price(command.price())
                .category(command.category())
                .imageWidth(command.imageWidth())
                .imageHeight(command.imageHeight())
                .build();

        Feed savedFeed = feedRepository.save(feed);

        FeedImage feedImage = FeedImage.builder()
                .feed(savedFeed)
                .s3ObjectKey(command.s3ObjectKey().trim())
                .build();

        feedImageRepository.save(feedImage);

        return savedFeed.getId();
    }

    /**
     * 피드 생성 정책 검증
     * - Bean Validation 이후, 도메인 레벨에서 최종 방어
     */
    private void validate(FeedCreateCommand command) {
        // content
        String content = normalizeContent(command.content());

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new GlobalException(FeedErrorCode.FEED_CONTENT_TOO_LONG);
        }

        // image
        if (command.s3ObjectKey() == null || command.s3ObjectKey().isBlank()) {
            throw new GlobalException(FeedErrorCode.FEED_IMAGE_REQUIRED);
        }
    }

    /**
     * content null-safe + trim
     */
    private String normalizeContent(String content) {
        return content == null ? "" : content.trim();
    }
}
