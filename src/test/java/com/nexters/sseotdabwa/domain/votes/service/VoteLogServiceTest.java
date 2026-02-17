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
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;
import com.nexters.sseotdabwa.domain.votes.service.command.VoteCreateCommand;

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
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());

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
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed1).choice(VoteChoice.YES).voteType(VoteType.USER).build());
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed2).choice(VoteChoice.NO).voteType(VoteType.USER).build());

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

    // ===== createVoteLog =====

    @Test
    @DisplayName("회원 투표 로그 생성 성공")
    void createVoteLog_user_success() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed = createFeed(feedOwner);
        VoteCreateCommand command = new VoteCreateCommand(voter, feed, VoteChoice.YES, VoteType.USER);

        // when
        VoteLog voteLog = voteLogService.createVoteLog(command);

        // then
        assertThat(voteLog.getId()).isNotNull();
        assertThat(voteLog.getUser().getId()).isEqualTo(voter.getId());
        assertThat(voteLog.getFeed().getId()).isEqualTo(feed.getId());
        assertThat(voteLog.getChoice()).isEqualTo(VoteChoice.YES);
        assertThat(voteLog.getVoteType()).isEqualTo(VoteType.USER);
    }

    @Test
    @DisplayName("게스트 투표 로그 생성 성공 - user null")
    void createVoteLog_guest_success() {
        // given
        User feedOwner = createUser();
        Feed feed = createFeed(feedOwner);
        VoteCreateCommand command = new VoteCreateCommand(null, feed, VoteChoice.NO, VoteType.SYSTEM);

        // when
        VoteLog voteLog = voteLogService.createVoteLog(command);

        // then
        assertThat(voteLog.getId()).isNotNull();
        assertThat(voteLog.getUser()).isNull();
        assertThat(voteLog.getFeed().getId()).isEqualTo(feed.getId());
        assertThat(voteLog.getChoice()).isEqualTo(VoteChoice.NO);
        assertThat(voteLog.getVoteType()).isEqualTo(VoteType.SYSTEM);
    }

    // ===== existsByUserAndFeed =====

    @Test
    @DisplayName("유저가 해당 피드에 투표했으면 true 반환")
    void existsByUserAndFeed_true() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed = createFeed(feedOwner);
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());

        // when
        boolean result = voteLogService.existsByUserAndFeed(voter.getId(), feed.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("유저가 해당 피드에 투표하지 않았으면 false 반환")
    void existsByUserAndFeed_false() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed = createFeed(feedOwner);

        // when
        boolean result = voteLogService.existsByUserAndFeed(voter.getId(), feed.getId());

        // then
        assertThat(result).isFalse();
    }

    // ===== findByUserIdAndFeedIds =====

    @Test
    @DisplayName("유저의 투표 로그를 피드 ID 목록으로 조회 성공")
    void findByUserIdAndFeedIds_success() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed1 = createFeed(feedOwner);
        Feed feed2 = createFeed(feedOwner);
        Feed feed3 = createFeed(feedOwner);
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed1).choice(VoteChoice.YES).voteType(VoteType.USER).build());
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed2).choice(VoteChoice.NO).voteType(VoteType.USER).build());

        // when
        List<VoteLog> result = voteLogService.findByUserIdAndFeedIds(
                voter.getId(), List.of(feed1.getId(), feed2.getId(), feed3.getId()));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(vl -> vl.getFeed().getId())
                .containsExactlyInAnyOrder(feed1.getId(), feed2.getId());
    }

    @Test
    @DisplayName("투표하지 않은 피드 목록으로 조회 시 빈 리스트 반환")
    void findByUserIdAndFeedIds_noVotes_returnsEmpty() {
        // given
        User feedOwner = createUser();
        User voter = createUser();
        Feed feed = createFeed(feedOwner);

        // when
        List<VoteLog> result = voteLogService.findByUserIdAndFeedIds(voter.getId(), List.of(feed.getId()));

        // then
        assertThat(result).isEmpty();
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
