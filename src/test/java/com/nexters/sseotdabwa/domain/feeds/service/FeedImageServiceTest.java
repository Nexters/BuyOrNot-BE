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
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedImageCreateInfo;
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
        feedImageService.saveAll(feed, List.of(new FeedImageCreateInfo("feeds/abc.jpg", 1080, 1350)));

        // then
        assertThat(feedImageRepository.count()).isEqualTo(1);

        FeedImage saved = feedImageRepository.findAll().get(0);
        assertThat(saved.getFeed().getId()).isEqualTo(feed.getId());
        assertThat(saved.getS3ObjectKey()).isEqualTo("feeds/abc.jpg");
        assertThat(saved.getImageWidth()).isEqualTo(1080);
        assertThat(saved.getImageHeight()).isEqualTo(1350);
    }

    @Test
    @DisplayName("피드 이미지 다중 저장 성공 - 이미지 3건 저장")
    void saveAll_multiple_success() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);
        List<FeedImageCreateInfo> keys = List.of(
                new FeedImageCreateInfo("key1.jpg", 100, 200),
                new FeedImageCreateInfo("key2.jpg", 100, 200),
                new FeedImageCreateInfo("key3.jpg", 100, 200)
        );

        // when
        feedImageService.saveAll(feed, keys);

        // then
        List<FeedImage> savedImages = feedImageRepository.findByFeedOrderByIdAsc(feed);
        assertThat(savedImages).hasSize(3);
        assertThat(savedImages).extracting(FeedImage::getS3ObjectKey)
                .containsExactly("key1.jpg", "key2.jpg", "key3.jpg");
    }

    @Test
    @DisplayName("피드 이미지 저장 성공 - 모든 s3ObjectKey에 trim 적용")
    void saveAll_trimApplied() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        // when
        feedImageService.saveAll(feed, List.of(
                new FeedImageCreateInfo("  feeds/trim1.jpg  ", 100, 200),
                new FeedImageCreateInfo("  feeds/trim2.jpg  ", 100, 200)
        ));

        // then
        List<FeedImage> savedImages = feedImageRepository.findByFeedOrderByIdAsc(feed);
        assertThat(savedImages).extracting(FeedImage::getS3ObjectKey)
                .containsExactly("feeds/trim1.jpg", "feeds/trim2.jpg");
    }

    @Test
    @DisplayName("피드 목록에 해당하는 이미지 삭제 성공")
    void deleteByFeeds_success() {
        // given
        User user = createUser();
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        feedImageRepository.save(FeedImage.builder().feed(feed1).s3ObjectKey("key1").imageWidth(100).imageHeight(100).build());
        feedImageRepository.save(FeedImage.builder().feed(feed2).s3ObjectKey("key2").imageWidth(100).imageHeight(100).build());

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
                .build());
    }
}
