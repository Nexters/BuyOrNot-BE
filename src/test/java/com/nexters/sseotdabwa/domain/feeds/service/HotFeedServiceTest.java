package com.nexters.sseotdabwa.domain.feeds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * 핫플 투표 목록 조회 (FeedService.findHotFeedsWithCursor / HotFeedCursor)
 * - 테스트 컨텍스트들이 같은 H2 DB를 공유하므로, 결과는 이 테스트에서 만든 피드로만 걸러서 검증한다.
 */
@SpringBootTest
@Transactional
class HotFeedServiceTest {

    @Autowired
    private FeedService feedService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("투표 수 많은 순 → 동률은 최신순, 투표 0개 피드는 맨 뒤에 최신순")
    void sortedByVotesThenIdDesc() {
        User user = createUser();
        Feed zeroOld = createFeed(user, FeedCategory.FASHION, 0, 0);
        Feed five = createFeed(user, FeedCategory.FASHION, 3, 2);
        Feed tenOld = createFeed(user, FeedCategory.FASHION, 10, 0);
        Feed tenNew = createFeed(user, FeedCategory.FASHION, 4, 6);
        Feed zeroNew = createFeed(user, FeedCategory.FASHION, 0, 0);

        List<Long> result = mine(feedService.findHotFeedsWithCursor(null, 50, null, null),
                tenOld, tenNew, five, zeroOld, zeroNew);

        assertThat(result).containsExactly(
                tenNew.getId(), tenOld.getId(), five.getId(), zeroNew.getId(), zeroOld.getId());
    }

    @Test
    @DisplayName("CLOSED·만료(스케줄러 미처리 OPEN)·신고·삭제 피드는 제외")
    void excludesClosedExpiredReportedDeleted() {
        User user = createUser();
        Feed open = createFeed(user, FeedCategory.FASHION, 1, 0);
        Feed closed = createFeed(user, FeedCategory.FASHION, 5, 0);
        closed.closeVote();
        Feed reported = createFeed(user, FeedCategory.FASHION, 5, 0);
        reported.report();
        Feed deleted = createFeed(user, FeedCategory.FASHION, 5, 0);
        deleted.deleteByReport();
        Feed expired = createFeed(user, FeedCategory.FASHION, 5, 0);
        entityManager.flush();
        setCreatedAt(expired.getId(), LocalDateTime.now().minusHours(Feed.VOTE_DEADLINE_HOURS).minusMinutes(1));

        List<Long> result = mine(feedService.findHotFeedsWithCursor(null, 50, null, null),
                open, closed, reported, deleted, expired);

        assertThat(result).containsExactly(open.getId());
    }

    @Test
    @DisplayName("마감 직전(48시간 미만) 피드는 포함")
    void includesFeedJustBeforeDeadline() {
        User user = createUser();
        Feed almost = createFeed(user, FeedCategory.FASHION, 1, 0);
        entityManager.flush();
        setCreatedAt(almost.getId(), LocalDateTime.now().minusHours(Feed.VOTE_DEADLINE_HOURS).plusMinutes(5));

        List<Long> result = mine(feedService.findHotFeedsWithCursor(null, 50, null, null), almost);

        assertThat(result).containsExactly(almost.getId());
    }

    @Test
    @DisplayName("카테고리 필터")
    void filtersByCategory() {
        User user = createUser();
        Feed book = createFeed(user, FeedCategory.BOOK, 1, 0);
        Feed food = createFeed(user, FeedCategory.FOOD, 2, 0);
        Feed fashion = createFeed(user, FeedCategory.FASHION, 3, 0);

        List<Long> result = mine(
                feedService.findHotFeedsWithCursor(null, 50, List.of(FeedCategory.BOOK, FeedCategory.FOOD), null),
                book, food, fashion);

        assertThat(result).containsExactly(food.getId(), book.getId());
    }

    @Test
    @DisplayName("차단 유저 피드 제외 + 게스트 피드는 유지 (카테고리 필터 조합 포함)")
    void excludesBlockedUsersButKeepsGuestFeeds() {
        User blocked = createUser();
        User other = createUser();
        Feed blockedFeed = createFeed(blocked, FeedCategory.BOOK, 9, 0);
        Feed otherFeed = createFeed(other, FeedCategory.BOOK, 2, 0);
        Feed guestFeed = createFeed(null, FeedCategory.BOOK, 1, 0);

        List<Long> excluded = List.of(blocked.getId());
        List<Long> noCategory = mine(feedService.findHotFeedsWithCursor(null, 50, null, excluded),
                blockedFeed, otherFeed, guestFeed);
        List<Long> withCategory = mine(feedService.findHotFeedsWithCursor(null, 50, List.of(FeedCategory.BOOK), excluded),
                blockedFeed, otherFeed, guestFeed);

        assertThat(noCategory).containsExactly(otherFeed.getId(), guestFeed.getId());
        assertThat(withCategory).containsExactly(otherFeed.getId(), guestFeed.getId());
    }

    @Test
    @DisplayName("동률 피드가 페이지 경계에 걸려도 커서로 끝까지 넘기면 누락/중복 없음")
    void paginationWithTiesHasNoGapOrDuplicate() {
        User user = createUser();
        List<Feed> created = new ArrayList<>();
        for (int i = 0; i < 3; i++) created.add(createFeed(user, FeedCategory.ETC, 2, 1)); // 3표 x3
        for (int i = 0; i < 3; i++) created.add(createFeed(user, FeedCategory.ETC, 0, 0)); // 0표 x3
        created.add(createFeed(user, FeedCategory.ETC, 5, 0));                              // 5표 x1

        List<Long> collected = new ArrayList<>();
        HotFeedCursor cursor = null;
        int pageSize = 2;
        for (int guard = 0; guard < 100; guard++) {
            List<Feed> page = feedService.findHotFeedsWithCursor(cursor, pageSize, List.of(FeedCategory.ETC), null);
            boolean hasNext = page.size() > pageSize;
            List<Feed> sliced = hasNext ? page.subList(0, pageSize) : page;
            sliced.forEach(f -> collected.add(f.getId()));
            if (!hasNext) break;
            cursor = HotFeedCursor.parse(HotFeedCursor.from(sliced.get(sliced.size() - 1)).encode());
        }

        Set<Long> createdIds = Set.copyOf(created.stream().map(Feed::getId).toList());
        List<Long> mine = collected.stream().filter(createdIds::contains).toList();
        assertThat(mine).hasSize(created.size()).doesNotHaveDuplicates();
        assertThat(collected).doesNotHaveDuplicates();
        assertThat(mine.get(0)).isEqualTo(created.get(6).getId()); // 5표가 맨 앞
    }

    @Test
    @DisplayName("커서 문자열 파싱 - 정상/빈 값/잘못된 형식")
    void parseCursor() {
        assertThat(HotFeedCursor.parse("12_345")).isEqualTo(new HotFeedCursor(12, 345));
        assertThat(HotFeedCursor.parse("0_1")).isEqualTo(new HotFeedCursor(0, 1));
        assertThat(HotFeedCursor.parse(null)).isNull();
        assertThat(HotFeedCursor.parse(" ")).isNull();
        assertThat(new HotFeedCursor(7, 99).encode()).isEqualTo("7_99");

        for (String invalid : List.of("123", "a_1", "1_b", "1_2_3", "-1_5", "3_0", "_5", "5_")) {
            assertThatThrownBy(() -> HotFeedCursor.parse(invalid))
                    .as(invalid)
                    .isInstanceOf(GlobalException.class)
                    .extracting(e -> ((GlobalException) e).getErrorCode())
                    .isEqualTo(FeedErrorCode.FEED_INVALID_CURSOR);
        }
    }

    @Test
    @DisplayName("voteClosedAt = createdAt + 48시간")
    void voteClosedAtIsCreatedAtPlusDeadline() {
        Feed feed = createFeed(createUser(), FeedCategory.FASHION, 0, 0);
        entityManager.flush();

        assertThat(feed.getVoteClosedAt()).isEqualTo(feed.getCreatedAt().plusHours(Feed.VOTE_DEADLINE_HOURS));
    }

    // ===== helpers =====

    private List<Long> mine(List<Feed> result, Feed... created) {
        Set<Long> ids = Set.copyOf(java.util.Arrays.stream(created).map(Feed::getId).toList());
        return result.stream().map(Feed::getId).filter(ids::contains).toList();
    }

    private Feed createFeed(User user, FeedCategory category, int yes, int no) {
        Feed.FeedBuilder builder = Feed.builder()
                .user(user)
                .content("핫플 테스트")
                .price(10000L)
                .category(category);
        if (user == null) {
            builder.guestNickname("게스트").guestPasswordHash("hash");
        }
        Feed feed = builder.build();
        for (int i = 0; i < yes; i++) feed.incrementYes();
        for (int i = 0; i < no; i++) feed.incrementNo();
        return feedRepository.saveAndFlush(feed);
    }

    private void setCreatedAt(Long feedId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE feeds SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, feedId)
                .executeUpdate();
        entityManager.clear();
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }
}
