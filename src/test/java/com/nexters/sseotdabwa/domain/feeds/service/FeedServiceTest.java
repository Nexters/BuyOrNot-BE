package com.nexters.sseotdabwa.domain.feeds.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FeedServiceTest {

    @Autowired
    private FeedService feedService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("유저 ID로 피드 목록 조회 성공")
    void findByUserId_success() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);

        // when
        List<Feed> feeds = feedService.findByUserId(user.getId());

        // then
        assertThat(feeds).hasSize(2);
        assertThat(feeds).extracting(Feed::getId)
                .containsExactlyInAnyOrder(feed1.getId(), feed2.getId());
    }

    @Test
    @DisplayName("유저 ID로 피드 삭제 성공")
    void deleteByUserId_success() {
        // given
        User user = createUser();
        createFeed(user);
        createFeed(user);

        // when
        feedService.deleteByUserId(user.getId());

        // then
        List<Feed> feeds = feedRepository.findByUserId(user.getId());
        assertThat(feeds).isEmpty();
    }

    @Test
    @DisplayName("피드가 없는 유저 ID로 조회 시 빈 리스트 반환")
    void findByUserId_noFeeds_returnsEmpty() {
        // given
        User user = createUser();

        // when
        List<Feed> feeds = feedService.findByUserId(user.getId());

        // then
        assertThat(feeds).isEmpty();
    }

    @Test
    @DisplayName("삭제된 피드 제외 전체 조회")
    void findAllExceptDeleted_excludesDeletedFeeds() {
        // given
        User user = createUser();
        Feed normalFeed = createFeed(user);
        Feed deletedFeed = createFeed(user);
        deletedFeed.deleteByReport();
        feedRepository.save(deletedFeed);

        // when
        List<Feed> result = feedService.findAllExceptDeleted();

        // then
        assertThat(result).extracting(Feed::getId)
                .contains(normalFeed.getId())
                .doesNotContain(deletedFeed.getId());
    }

    @Test
    @DisplayName("내 피드 최신순 조회")
    void findByUserIdOrderByCreatedAtDesc_success() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);

        // when
        List<Feed> result = feedService.findByUserIdOrderByCreatedAtDesc(user.getId());

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("피드 단건 조회 성공")
    void findById_success() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        // when
        Feed result = feedService.findById(feed.getId());

        // then
        assertThat(result.getId()).isEqualTo(feed.getId());
        assertThat(result.getContent()).isEqualTo(feed.getContent());
    }

    @Test
    @DisplayName("존재하지 않는 피드 조회 시 FEED_NOT_FOUND 예외")
    void findById_notFound() {
        // when & then
        assertThatThrownBy(() -> feedService.findById(999L))
                .isInstanceOf(GlobalException.class)
                .hasMessage("피드를 찾을 수 없습니다.");
    }

    // ===== findByIdWithLock =====

    @Test
    @DisplayName("비관적 락으로 피드 조회 성공")
    void findByIdWithLock_success() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        // when
        Feed result = feedService.findByIdWithLock(feed.getId());

        // then
        assertThat(result.getId()).isEqualTo(feed.getId());
        assertThat(result.getContent()).isEqualTo(feed.getContent());
    }

    @Test
    @DisplayName("비관적 락 조회 시 존재하지 않는 피드면 FEED_NOT_FOUND 예외")
    void findByIdWithLock_notFound() {
        // when & then
        assertThatThrownBy(() -> feedService.findByIdWithLock(999L))
                .isInstanceOf(GlobalException.class)
                .hasMessage("피드를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("피드 물리 삭제 확인")
    void delete_success() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);
        Long feedId = feed.getId();

        // when
        feedService.delete(feed);
        feedRepository.flush();

        // then
        assertThat(feedRepository.findById(feedId)).isEmpty();
    }

    // ===== closeExpiredFeeds =====

    @Test
    @DisplayName("48시간 초과 OPEN 피드 → CLOSED 전환")
    void closeExpiredFeeds_closesExpiredOpenFeeds() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);
        setCreatedAt(feed.getId(), LocalDateTime.now().minusHours(49));

        // when
        int closedCount = feedService.closeExpiredFeeds();
        entityManager.clear();

        // then
        assertThat(closedCount).isEqualTo(1);
        Feed updated = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updated.getFeedStatus()).isEqualTo(FeedStatus.CLOSED);
    }

    @Test
    @DisplayName("48시간 이내 OPEN 피드 → OPEN 유지")
    void closeExpiredFeeds_keepsRecentOpenFeeds() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);
        setCreatedAt(feed.getId(), LocalDateTime.now().minusHours(47));

        // when
        int closedCount = feedService.closeExpiredFeeds();
        entityManager.clear();

        // then
        assertThat(closedCount).isEqualTo(0);
        Feed updated = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updated.getFeedStatus()).isEqualTo(FeedStatus.OPEN);
    }

    @Test
    @DisplayName("이미 CLOSED 피드 → 영향 없음")
    void closeExpiredFeeds_ignoresAlreadyClosedFeeds() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);
        feed.closeVote();
        feedRepository.save(feed);
        setCreatedAt(feed.getId(), LocalDateTime.now().minusHours(49));

        // when
        int closedCount = feedService.closeExpiredFeeds();

        // then
        assertThat(closedCount).isEqualTo(0);
    }

    private void setCreatedAt(Long feedId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE feeds SET created_at = :createdAt WHERE id = :feedId")
                .setParameter("createdAt", createdAt)
                .setParameter("feedId", feedId)
                .executeUpdate();
        entityManager.flush();
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    // ===== findAllExceptDeletedWithCursor =====

    @Test
    @DisplayName("커서 페이지네이션 - 첫 페이지 조회 (cursor=null)")
    void findAllExceptDeletedWithCursor_firstPage() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        Feed feed3 = createFeed(user);

        // when
        List<Feed> result = feedService.findAllExceptDeletedWithCursor(null, 2);

        // then
        assertThat(result).hasSize(3); // size+1 = 3건 조회
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId()); // ID 내림차순
    }

    @Test
    @DisplayName("커서 페이지네이션 - 다음 페이지 조회 (cursor 지정)")
    void findAllExceptDeletedWithCursor_nextPage() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        Feed feed3 = createFeed(user);

        // when - feed3의 ID를 커서로 지정하면 feed3보다 작은 ID만
        List<Feed> result = feedService.findAllExceptDeletedWithCursor(feed3.getId(), 2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(feed ->
                assertThat(feed.getId()).isLessThan(feed3.getId()));
    }

    @Test
    @DisplayName("커서 페이지네이션 - 마지막 페이지는 남은 건수만 반환")
    void findAllExceptDeletedWithCursor_lastPage() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);

        // when - size=5 요청했지만 1건만 존재
        List<Feed> result = feedService.findAllExceptDeletedWithCursor(null, 5);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("커서 페이지네이션 - DELETED 상태 피드 제외")
    void findAllExceptDeletedWithCursor_excludesDeleted() {
        // given
        User user = createUser();
        Feed normalFeed = createFeed(user);
        Feed deletedFeed = createFeed(user);
        deletedFeed.deleteByReport();
        feedRepository.save(deletedFeed);

        // when
        List<Feed> result = feedService.findAllExceptDeletedWithCursor(null, 10);

        // then
        assertThat(result).extracting(Feed::getId)
                .contains(normalFeed.getId())
                .doesNotContain(deletedFeed.getId());
    }

    @Test
    @DisplayName("커서 페이지네이션 - 피드 없을 때 빈 리스트")
    void findAllExceptDeletedWithCursor_emptyResult() {
        // when
        List<Feed> result = feedService.findAllExceptDeletedWithCursor(null, 10);

        // then
        assertThat(result).isEmpty();
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
