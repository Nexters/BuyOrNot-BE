package com.nexters.sseotdabwa.api.feeds.facade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedReviewService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;
import com.nexters.sseotdabwa.domain.storage.service.S3StorageService;
import com.nexters.sseotdabwa.domain.users.entity.User;
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

    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final FeedReviewService feedReviewService;
    private final VoteLogService voteLogService;
    private final S3StorageService s3StorageService;

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
     * 피드 리스트 조회 (비로그인 가능)
     * - 인증된 경우: 투표 상태(hasVoted, myVoteChoice) 포함
     * - 비인증인 경우: 투표 상태 없음
     */
    @Transactional(readOnly = true)
    public List<FeedResponse> getFeedList(User user) {
        List<Feed> feeds = feedService.findAllExceptDeleted();
        List<FeedImage> feedImages = feedImageService.findByFeeds(feeds);
        Map<Long, FeedImage> imageMap = feedImages.stream()
                .collect(Collectors.toMap(fi -> fi.getFeed().getId(), fi -> fi));

        if (user == null || feeds.isEmpty()) {
            return feeds.stream()
                    .map(feed -> FeedResponse.of(feed, imageMap.get(feed.getId())))
                    .toList();
        }

        List<Long> feedIds = feeds.stream().map(Feed::getId).toList();
        Map<Long, VoteChoice> voteMap = voteLogService.findByUserIdAndFeedIds(user.getId(), feedIds)
                .stream()
                .collect(Collectors.toMap(vl -> vl.getFeed().getId(), vl -> vl.getChoice()));

        return feeds.stream()
                .map(feed -> {
                    VoteChoice myChoice = voteMap.get(feed.getId());
                    boolean hasVoted = myChoice != null;
                    return FeedResponse.of(feed, imageMap.get(feed.getId()), hasVoted, myChoice);
                })
                .toList();
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
