package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FeedImageServiceTest {

    @Autowired
    private FeedImageService feedImageService;

    @Autowired
    private FeedImageRepository feedImageRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("피드 이미지 저장 성공 - FeedImage 1건 저장")
    void save_success() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        // when
        feedImageService.save(feed, "feeds/abc.jpg");

        // then
        assertThat(feedImageRepository.count()).isEqualTo(1);

        FeedImage saved = feedImageRepository.findAll().get(0);
        assertThat(saved.getFeed().getId()).isEqualTo(feed.getId());
        assertThat(saved.getS3ObjectKey()).isEqualTo("feeds/abc.jpg");
    }

    @Test
    @DisplayName("피드 이미지 저장 성공 - s3ObjectKey trim 적용")
    void save_trimApplied() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        // when
        feedImageService.save(feed, "  feeds/trim.jpg  ");

        // then
        FeedImage saved = feedImageRepository.findAll().get(0);
        assertThat(saved.getS3ObjectKey()).isEqualTo("feeds/trim.jpg");
    }

    @Test
    @DisplayName("피드 목록에 해당하는 이미지 삭제 성공")
    void deleteByFeeds_success() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        feedImageRepository.save(FeedImage.builder().feed(feed1).s3ObjectKey("key1").build());
        feedImageRepository.save(FeedImage.builder().feed(feed2).s3ObjectKey("key2").build());

        // when
        feedImageService.deleteByFeeds(List.of(feed1, feed2));

        // then
        assertThat(feedImageRepository.count()).isZero();
    }

    @Test
    @DisplayName("빈 피드 목록으로 삭제 시 에러 없음")
    void deleteByFeeds_emptyList_noError() {
        // when & then
        feedImageService.deleteByFeeds(List.of());
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
