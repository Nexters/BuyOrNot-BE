package com.nexters.sseotdabwa.api.feeds.facade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedReviewService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;
import com.nexters.sseotdabwa.domain.notifications.service.NotificationService;
import com.nexters.sseotdabwa.domain.storage.service.S3StorageService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feed 생성 흐름 조합 Facade
 * - FeedService(Feed 저장) + FeedImageService(이미지 저장) 조합
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final FeedReviewService feedReviewService;
    private final VoteLogService voteLogService;
    private final S3StorageService s3StorageService;
    private final NotificationService notificationService;
    private final AwsProperties awsProperties;

    /**
     * 피드 생성 + 피드 이미지 저장
     */
    @Transactional
    public FeedCreateResponse createFeed(User user, FeedCreateRequest request) {
        FeedCreateCommand command = new FeedCreateCommand(
                user,
                request.content(),
                request.price(),
                request.category(),
                request.imageWidth(),
                request.imageHeight(),
                request.s3ObjectKey()
        );

        // 1) Feed 저장
        Feed savedFeed = feedService.createFeed(command);

        // 2) FeedImage 저장
        feedImageService.save(savedFeed, command.s3ObjectKey());

        return new FeedCreateResponse(savedFeed.getId());
    }

    /**
     * 피드 단건 조회 (비로그인 가능)
     * - 인증된 경우: 투표 상태(hasVoted, myVoteChoice) 포함
     * - 비인증인 경우: 투표 상태 없음
     */
    @Transactional(readOnly = true)
    public FeedResponse getFeedDetail(User user, Long feedId) {
        Feed feed = feedService.findById(feedId);
        FeedImage feedImage = feedImageService.findByFeed(feed).orElse(null);
        String viewUrl = buildViewUrl(feedImage);

        if (user == null) {
            return FeedResponse.of(feed, feedImage, viewUrl);
        }

        List<VoteLog> voteLogs = voteLogService.findByUserIdAndFeedIds(user.getId(), List.of(feedId));
        VoteChoice myChoice = voteLogs.isEmpty() ? null : voteLogs.get(0).getChoice();
        boolean hasVoted = myChoice != null;
        return FeedResponse.of(feed, feedImage, viewUrl, hasVoted, myChoice);
    }

    /**
     * 피드 리스트 조회 (비로그인 가능, 커서 기반 페이지네이션)
     * - 인증된 경우: 투표 상태(hasVoted, myVoteChoice) 포함
     * - 비인증인 경우: 투표 상태 없음
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<FeedResponse> getFeedList(User user, Long cursor, Integer size, FeedStatus feedStatus) {
        int pageSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        List<Feed> feeds = feedService.findAllExceptDeletedWithCursor(cursor, pageSize, feedStatus);

        boolean hasNext = feeds.size() > pageSize;
        List<Feed> slicedFeeds = hasNext ? feeds.subList(0, pageSize) : feeds;

        List<FeedImage> feedImages = feedImageService.findByFeeds(slicedFeeds);
        Map<Long, FeedImage> imageMap = feedImages.stream()
                .collect(Collectors.toMap(fi -> fi.getFeed().getId(), fi -> fi));

        List<FeedResponse> content;
        if (user == null || slicedFeeds.isEmpty()) {
            content = slicedFeeds.stream()
                    .map(feed -> {
                        FeedImage img = imageMap.get(feed.getId());
                        return FeedResponse.of(feed, img, buildViewUrl(img));
                    })
                    .toList();
        } else {
            List<Long> feedIds = slicedFeeds.stream().map(Feed::getId).toList();
            Map<Long, VoteChoice> voteMap = voteLogService.findByUserIdAndFeedIds(user.getId(), feedIds)
                    .stream()
                    .collect(Collectors.toMap(vl -> vl.getFeed().getId(), vl -> vl.getChoice()));

            content = slicedFeeds.stream()
                    .map(feed -> {
                        FeedImage img = imageMap.get(feed.getId());
                        VoteChoice myChoice = voteMap.get(feed.getId());
                        boolean hasVoted = myChoice != null;
                        return FeedResponse.of(feed, img, buildViewUrl(img), hasVoted, myChoice);
                    })
                    .toList();
        }

        Long nextCursor = hasNext ? slicedFeeds.get(slicedFeeds.size() - 1).getId() : null;
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    private String buildViewUrl(FeedImage feedImage) {
        if (feedImage == null) {
            return null;
        }
        String domain = awsProperties.cloudfront().domain();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/" + feedImage.getS3ObjectKey();
    }

    /**
     * 피드 삭제 (물리 삭제 + S3 이미지 삭제)
     */
    @Transactional
    public void deleteFeed(User user, Long feedId) {
        Feed feed = feedService.findById(feedId);
        if (!feed.isOwner(user)) {
            throw new GlobalException(FeedErrorCode.FEED_DELETE_FORBIDDEN);
        }

        // S3 삭제를 위해 s3ObjectKey 미리 조회
        String s3ObjectKey = feedImageService.findByFeed(feed)
                .map(FeedImage::getS3ObjectKey)
                .orElse(null);

        notificationService.deleteByFeed(feed);
        voteLogService.deleteByFeed(feed);
        feedImageService.deleteByFeed(feed);
        feedReviewService.deleteByFeed(feed);
        feedService.delete(feed);

        // S3 오브젝트 삭제 (실패해도 DB 트랜잭션은 유지)
        if (s3ObjectKey != null) {
            try {
                s3StorageService.deleteObject(s3ObjectKey);
            } catch (Exception e) {
                log.warn("S3 오브젝트 삭제 실패. key={}", s3ObjectKey, e);
            }
        }
    }

    /**
     * 피드 신고
     */
    @Transactional
    public void reportFeed(User user, Long feedId) {
        Feed feed = feedService.findById(feedId);
        if (feed.isOwner(user)) {
            throw new GlobalException(FeedErrorCode.FEED_SELF_REPORT);
        }
        if (feed.isReported()) {
            throw new GlobalException(FeedErrorCode.FEED_ALREADY_REPORTED);
        }
        feedService.report(feed);
    }
}
