package com.nexters.sseotdabwa.domain.feeds.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
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
        // 1. content 길이 검증
        String content = normalizeContent(command.content());
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new GlobalException(FeedErrorCode.FEED_CONTENT_TOO_LONG);
        }

        // 2. 이미지 리스트 자체 검증 (null 또는 비어있음)
        if (command.s3ObjectKeys() == null || command.s3ObjectKeys().isEmpty()) {
            throw new GlobalException(FeedErrorCode.FEED_IMAGE_REQUIRED);
        }

        // 3. 이미지 개수 제한 검증 (최대 3장)
        if (command.s3ObjectKeys().size() > 3) {
            throw new GlobalException(FeedErrorCode.FEED_IMAGE_LIMIT_EXCEEDED);
        }

        // 4. 이미지 원소 내부 검증 (원소가 null이거나 공백만 있는 경우 방지)
        // 리스트 내 단 하나라도 유효하지 않은 키가 있다면 예외를 던집니다.
        if (command.s3ObjectKeys().stream().anyMatch(key -> key == null || key.isBlank())) {
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

    public List<Feed> findAllExceptDeletedWithCursor(Long cursor, int size, FeedStatus feedStatus) {
        Pageable pageable = PageRequest.ofSize(size + 1);
        if (feedStatus != null) {
            if (cursor == null) {
                return feedRepository.findByFeedStatusAndReportStatusNotOrderByCreatedAtDescIdDesc(feedStatus, ReportStatus.DELETED, pageable);
            }
            return feedRepository.findByIdLessThanAndFeedStatusAndReportStatusNotOrderByCreatedAtDescIdDesc(cursor, feedStatus, ReportStatus.DELETED, pageable);
        }
        if (cursor == null) {
            return feedRepository.findByReportStatusNotOrderByCreatedAtDescIdDesc(ReportStatus.DELETED, pageable);
        }
        return feedRepository.findByIdLessThanAndReportStatusNotOrderByCreatedAtDescIdDesc(cursor, ReportStatus.DELETED, pageable);
    }

    public List<Feed> findAllExceptDeletedWithCursor(Long cursor, int size, FeedStatus feedStatus, List<Long> excludedUserIds) {
        if (excludedUserIds == null || excludedUserIds.isEmpty()) {
            return findAllExceptDeletedWithCursor(cursor, size, feedStatus);
        }
        Pageable pageable = PageRequest.ofSize(size + 1);
        if (feedStatus != null) {
            if (cursor == null) {
                return feedRepository.findByFeedStatusAndReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(feedStatus, excludedUserIds, pageable);
            }
            return feedRepository.findByIdLessThanAndFeedStatusAndReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(cursor, feedStatus, excludedUserIds, pageable);
        }
        if (cursor == null) {
            return feedRepository.findByReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(excludedUserIds, pageable);
        }
        return feedRepository.findByIdLessThanAndReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(cursor, excludedUserIds, pageable);
    }

    public List<Feed> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return feedRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Feed> findByUserIdWithCursor(Long userId, Long cursor, int size, FeedStatus feedStatus) {
        Pageable pageable = PageRequest.ofSize(size + 1);
        if (feedStatus != null) {
            if (cursor == null) {
                return feedRepository.findByUserIdAndFeedStatusOrderByCreatedAtDescIdDesc(userId, feedStatus, pageable);
            }
            return feedRepository.findByUserIdAndIdLessThanAndFeedStatusOrderByCreatedAtDescIdDesc(userId, cursor, feedStatus, pageable);
        }
        if (cursor == null) {
            return feedRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        }
        return feedRepository.findByUserIdAndIdLessThanOrderByCreatedAtDescIdDesc(userId, cursor, pageable);
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

    /**
     * 만료된 OPEN 피드들을 CLOSED로 전환하고, 마감된 feedId 리스트를 반환한다.
     * - count가 아닌 대상 id가 필요 (알림 생성).
     */
    @Transactional
    public List<Long> closeExpiredFeedsAndReturnIds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(48);

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
