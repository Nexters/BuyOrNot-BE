package com.nexters.sseotdabwa.api.users.facade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedReviewService;
import com.nexters.sseotdabwa.domain.auth.service.RefreshTokenService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.service.UserService;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관련 비즈니스 로직을 조합하는 Facade
 */
@Component
@RequiredArgsConstructor
public class UserFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final FeedReviewService feedReviewService;
    private final VoteLogService voteLogService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AwsProperties awsProperties;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    public UserResponse getMyInfo(User user) {
        return UserResponse.from(user);
    }

    /**
     * 회원 탈퇴
     * - 유저의 Feed에 달린 VoteLog 삭제
     * - 유저가 다른 Feed에 투표한 VoteLog 삭제
     * - 유저의 Feed에 연결된 FeedImage, FeedReview 삭제
     * - 유저의 Feed 삭제
     * - User 레코드 삭제
     */
    @Transactional
    public UserWithdrawResponse withdraw(User user) {
        UserWithdrawResponse response = UserWithdrawResponse.from(user);

        List<Feed> feeds = feedService.findByUserId(user.getId());

        if (!feeds.isEmpty()) {
            voteLogService.deleteByFeeds(feeds);
            feedImageService.deleteByFeeds(feeds);
            feedReviewService.deleteByFeeds(feeds);
        }

        voteLogService.deleteByUserId(user.getId());
        feedService.deleteByUserId(user.getId());
        refreshTokenService.deleteByUserId(user.getId());
        userService.delete(user);

        return response;
    }

    /**
     * 내 피드 조회 (커서 기반 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<FeedResponse> getMyFeeds(User user, Long cursor, Integer size, FeedStatus feedStatus) {
        int pageSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        List<Feed> feeds = feedService.findByUserIdWithCursor(user.getId(), cursor, pageSize, feedStatus);

        boolean hasNext = feeds.size() > pageSize;
        List<Feed> slicedFeeds = hasNext ? feeds.subList(0, pageSize) : feeds;

        List<FeedImage> feedImages = feedImageService.findByFeeds(slicedFeeds);
        Map<Long, FeedImage> imageMap = feedImages.stream()
                .collect(Collectors.toMap(fi -> fi.getFeed().getId(), fi -> fi));

        List<Long> feedIds = slicedFeeds.stream().map(Feed::getId).toList();
        Map<Long, VoteChoice> voteMap = voteLogService.findByUserIdAndFeedIds(user.getId(), feedIds)
                .stream()
                .collect(Collectors.toMap(vl -> vl.getFeed().getId(), vl -> vl.getChoice()));

        List<FeedResponse> content = slicedFeeds.stream()
                .map(feed -> {
                    FeedImage img = imageMap.get(feed.getId());
                    VoteChoice myChoice = voteMap.get(feed.getId());
                    boolean hasVoted = myChoice != null;
                    return FeedResponse.of(feed, img, buildViewUrl(img), hasVoted, myChoice);
                })
                .toList();

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
     * FCM 토큰 등록/갱신
     */
    @Transactional
    public void updateFcmToken(User user, FcmTokenRequest request) {
        userService.updateFcmToken(user.getId(), request.fcmToken());
    }
}
