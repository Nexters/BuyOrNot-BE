package com.nexters.sseotdabwa.api.users.facade;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedReviewService;
import com.nexters.sseotdabwa.domain.auth.service.RefreshTokenService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.service.UserService;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 관련 비즈니스 로직을 조합하는 Facade
 */
@Component
@RequiredArgsConstructor
public class UserFacade {

    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final FeedReviewService feedReviewService;
    private final VoteLogService voteLogService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

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
}
