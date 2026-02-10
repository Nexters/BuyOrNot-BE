package com.nexters.sseotdabwa.domain.votes.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class VoteLogServiceTest {

    @Autowired
    private VoteLogService voteLogService;

    @Autowired
    private VoteLogRepository voteLogRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("유저 ID로 투표 로그 삭제 성공")
    void deleteByUserId_success() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed = createFeed(feedOwner);
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).build());

        // when
        voteLogService.deleteByUserId(voter.getId());

        // then
        assertThat(voteLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("피드 목록에 달린 투표 로그 삭제 성공")
    void deleteByFeeds_success() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed1 = createFeed(feedOwner);
        Feed feed2 = createFeed(feedOwner);
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed1).choice(VoteChoice.YES).build());
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed2).choice(VoteChoice.NO).build());

        // when
        voteLogService.deleteByFeeds(List.of(feed1, feed2));

        // then
        assertThat(voteLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("빈 피드 목록으로 삭제 시 에러 없음")
    void deleteByFeeds_emptyList_noError() {
        // when & then
        voteLogService.deleteByFeeds(List.of());
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private Feed createFeed(User user) {
        return feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build());
    }
}
