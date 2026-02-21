package com.nexters.sseotdabwa.domain.feeds.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 피드 도메인 서비스
 * - Feed 생성/조회 등 "Feed" 자체의 핵심 로직만 담당
 *
 * 정책:
 * - content: 100자 이하
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int MAX_CONTENT_LENGTH = 100;

    private final FeedRepository feedRepository;

    /**
     * Feed 생성 (Feed 엔티티 저장만 담당)
     * - 이미지 저장은 FeedFacade에서 FeedImageService로 위임/조합한다.
     */
    @Transactional
    public Feed createFeed(FeedCreateCommand command) {
        validate(command);

        Feed feed = Feed.builder()
                .user(command.user())
                .content(normalizeContent(command.content()))
                .price(command.price())
                .category(command.category())
                .imageWidth(command.imageWidth())
                .imageHeight(command.imageHeight())
                .build();

        return feedRepository.save(feed);
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

    public List<Feed> findByUserId(Long userId) {
        return feedRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        feedRepository.deleteByUserId(userId);
    }

    public Feed findById(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new GlobalException(FeedErrorCode.FEED_NOT_FOUND));
    }

    public List<Feed> findAllExceptDeleted() {
        return feedRepository.findByReportStatusNotOrderByCreatedAtDesc(ReportStatus.DELETED);
    }

    public List<Feed> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return feedRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(Feed feed) {
        feedRepository.delete(feed);
    }

    @Transactional
    public void report(Feed feed) {
        feed.report();
    }

    @Transactional
    public Feed findByIdWithLock(Long feedId) {
        return feedRepository.findByIdWithPessimisticLock(feedId)
                .orElseThrow(() -> new GlobalException(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Transactional
    public int closeExpiredFeeds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(48);
        return feedRepository.closeExpiredFeeds(cutoff, now);
    }

    /**
     * 만료된 OPEN 피드들을 CLOSED로 전환하고, 마감된 feedId 리스트를 반환한다.
     * - count가 아닌 대상 id가 필요 (알림 생성).
     */
    @Transactional
    public List<Long> closeExpiredFeedsAndReturnIds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(1);

        List<ReportStatus> excluded = Arrays.asList(ReportStatus.DELETED, ReportStatus.REPORTED);

        List<Long> feedIds = feedRepository.findExpiredOpenFeedIds(cutoff, excluded);
        if (feedIds.isEmpty()) {
            return Collections.emptyList();
        }

        feedRepository.closeFeedsByIds(feedIds, now);
        return feedIds;
    }

    @Transactional(readOnly = true)
    public List<Feed> findByIds(List<Long> feedIds) {
        if (feedIds == null || feedIds.isEmpty()) {
            return List.of();
        }
        return feedRepository.findAllById(feedIds);
    }
}
