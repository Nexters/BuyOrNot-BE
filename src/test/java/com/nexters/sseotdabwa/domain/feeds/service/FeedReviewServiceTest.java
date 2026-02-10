package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedReview;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedReviewRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FeedReviewServiceTest {

    @Autowired
    private FeedReviewService feedReviewService;

    @Autowired
    private FeedReviewRepository feedReviewRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("피드 목록에 해당하는 리뷰 삭제 성공")
    void deleteByFeeds_success() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        feedReviewRepository.save(FeedReview.builder().feed(feed1).content("리뷰1").build());
        feedReviewRepository.save(FeedReview.builder().feed(feed2).content("리뷰2").build());

        // when
        feedReviewService.deleteByFeeds(List.of(feed1, feed2));

        // then
        assertThat(feedReviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("빈 피드 목록으로 삭제 시 에러 없음")
    void deleteByFeeds_emptyList_noError() {
        // when & then
        feedReviewService.deleteByFeeds(List.of());
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
