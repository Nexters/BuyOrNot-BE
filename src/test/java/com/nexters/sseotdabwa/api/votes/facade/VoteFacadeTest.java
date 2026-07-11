package com.nexters.sseotdabwa.api.votes.facade;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.dto.VoteResponse;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class VoteFacadeTest {

    @Autowired
    private VoteFacade voteFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private VoteLogRepository voteLogRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void cleanup() {
        if (!TestTransaction.isActive()) {
            notificationRepository.deleteAll();
            voteLogRepository.deleteAll();
            feedRepository.deleteAll();
            userRepository.deleteAll();
        }
    }

    // ===== 회원 투표 =====

    @Test
    @DisplayName("회원 YES 투표 성공")
    void vote_yes_success() {
        // given
        User owner = createUser();
        User voter = createUserWithProfile("https://cdn.example.com/profile1.png");
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when
        VoteResponse response = voteFacade.vote(voter, feed.getId(), request);

        // then
        assertThat(response.feedId()).isEqualTo(feed.getId());
        assertThat(response.choice()).isEqualTo(VoteChoice.YES);
        assertThat(response.yesCount()).isEqualTo(1L);
        assertThat(response.noCount()).isEqualTo(0L);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.myProfileImage()).isEqualTo("https://cdn.example.com/profile1.png");
    }

    @Test
    @DisplayName("회원 NO 투표 성공")
    void vote_no_success() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.NO);

        // when
        VoteResponse response = voteFacade.vote(voter, feed.getId(), request);

        // then
        assertThat(response.feedId()).isEqualTo(feed.getId());
        assertThat(response.choice()).isEqualTo(VoteChoice.NO);
        assertThat(response.yesCount()).isEqualTo(0L);
        assertThat(response.noCount()).isEqualTo(1L);
        assertThat(response.totalCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원 중복 투표 시 VOTE_001 에러")
    void vote_duplicate_throwsVote001() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.YES);
        voteFacade.vote(voter, feed.getId(), request);

        // when & then
        assertThatThrownBy(() -> voteFacade.vote(voter, feed.getId(), request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("이미 투표한 피드입니다.");
    }

    @Test
    @DisplayName("마감된 피드에 투표 시 VOTE_002 에러")
    void vote_closedFeed_throwsVote002() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        feed.closeVote();
        feedRepository.save(feed);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        assertThatThrownBy(() -> voteFacade.vote(voter, feed.getId(), request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("마감된 피드에는 투표할 수 없습니다.");
    }

    @Test
    @DisplayName("본인 피드에 투표 시 VOTE_003 에러")
    void vote_ownFeed_throwsVote003() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        assertThatThrownBy(() -> voteFacade.vote(owner, feed.getId(), request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("본인의 피드에는 투표할 수 없습니다.");
    }

    // ===== 게스트 투표 =====

    @Test
    @DisplayName("게스트 투표 성공")
    void guestVote_success() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when
        VoteResponse response = voteFacade.guestVote(feed.getId(), request);

        // then
        assertThat(response.feedId()).isEqualTo(feed.getId());
        assertThat(response.choice()).isEqualTo(VoteChoice.YES);
        assertThat(response.yesCount()).isEqualTo(1L);
        assertThat(response.noCount()).isEqualTo(0L);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.myProfileImage()).isNull();
    }

    @Test
    @DisplayName("게스트 중복 투표 허용")
    void guestVote_duplicate_allowed() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.YES);
        voteFacade.guestVote(feed.getId(), request);

        // when
        VoteResponse response = voteFacade.guestVote(feed.getId(), request);

        // then
        assertThat(response.yesCount()).isEqualTo(2L);
        assertThat(response.totalCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("게스트 마감 피드 투표 시 VOTE_002 에러")
    void guestVote_closedFeed_throwsVote002() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        feed.closeVote();
        feedRepository.save(feed);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        assertThatThrownBy(() -> voteFacade.guestVote(feed.getId(), request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("마감된 피드에는 투표할 수 없습니다.");
    }

    // ===== 마일스톤 알림 =====

    @Test
    @DisplayName("회원 투표 1번째 - MY_FEED_VOTED_1 알림 생성")
    void vote_first_creates_voted1_notification() {
        // given - REQUIRES_NEW가 참조할 수 있도록 선커밋
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // when
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));

        // then
        assertThat(notificationRepository.existsByUserIdAndFeedIdAndType(
                owner.getId(), feed.getId(), NotificationType.MY_FEED_VOTED_1)).isTrue();
    }

    @Test
    @DisplayName("회원 투표 10번째 - MY_FEED_VOTED_10 알림 생성")
    void vote_tenth_creates_voted10_notification() {
        // given - REQUIRES_NEW가 참조할 수 있도록 선커밋
        User owner = createUser();
        Feed feed = createFeed(owner);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // 9명 투표 (게스트로 카운트만 채움)
        for (int i = 0; i < 9; i++) {
            voteFacade.guestVote(feed.getId(), new VoteRequest(VoteChoice.YES));
        }

        // when - 10번째 투표 (회원)
        User voter = createUser();
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));

        // then
        assertThat(notificationRepository.existsByUserIdAndFeedIdAndType(
                owner.getId(), feed.getId(), NotificationType.MY_FEED_VOTED_10)).isTrue();
    }

    @Test
    @DisplayName("게스트 투표는 알림을 생성하지 않는다")
    void guestVote_does_not_create_notification() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);

        // when
        voteFacade.guestVote(feed.getId(), new VoteRequest(VoteChoice.YES));

        // then
        assertThat(notificationRepository.count()).isZero();
    }

    // ===== 48시간 만료 검증 =====

    @Test
    @DisplayName("48시간 초과 피드에 회원 투표 시 VOTE_002 에러")
    void vote_expiredFeed_throwsVote002() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        setCreatedAt(feed.getId(), LocalDateTime.now().minusHours(49));
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        assertThatThrownBy(() -> voteFacade.vote(voter, feed.getId(), request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("마감된 피드에는 투표할 수 없습니다.");
    }

    @Test
    @DisplayName("48시간 초과 피드에 게스트 투표 시 VOTE_002 에러")
    void guestVote_expiredFeed_throwsVote002() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        setCreatedAt(feed.getId(), LocalDateTime.now().minusHours(49));
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        assertThatThrownBy(() -> voteFacade.guestVote(feed.getId(), request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("마감된 피드에는 투표할 수 없습니다.");
    }

    // ===== Helper Methods =====

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private User createUserWithProfile(String profileImage) {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .profileImage(profileImage)
                .build());
    }

    private Feed createFeed(User user) {
        return feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build());
    }

    private void setCreatedAt(Long feedId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE feeds SET created_at = :createdAt WHERE id = :feedId")
                .setParameter("createdAt", createdAt)
                .setParameter("feedId", feedId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}
