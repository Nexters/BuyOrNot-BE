package com.nexters.sseotdabwa.domain.feeds.service;

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
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

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
