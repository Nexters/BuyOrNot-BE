package com.nexters.sseotdabwa.domain.feeds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexters.sseotdabwa.api.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;
import com.nexters.sseotdabwa.domain.users.entity.User;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedImageRepository feedImageRepository;

    @InjectMocks
    private FeedService feedService;

    @Test
    @DisplayName("피드 생성 성공 - Feed/FeedImage 저장 후 feedId 반환")
    void createFeed_success() {
        // given
        User user = User.builder()
                .socialId("test-social-id")
                .nickname("테스트")
                .build();

        FeedCreateCommand command = new FeedCreateCommand(
                user,
                "  내용입니다  ",
                10000L,
                FeedCategory.FOOD,
                1080,
                720,
                "  feeds/abc.jpg  "
        );

        // FeedRepository.save()가 id=1L인 Feed를 반환하도록 스텁
        Feed savedFeed = Feed.builder()
                .user(user)
                .content("내용입니다")
                .price(10000L)
                .category(FeedCategory.FOOD)
                .imageWidth(1080)
                .imageHeight(720)
                .build();

        Feed savedFeedSpy = spy(savedFeed);
        given(savedFeedSpy.getId()).willReturn(1L);

        given(feedRepository.save(any(Feed.class))).willReturn(savedFeedSpy);
        given(feedImageRepository.save(any(FeedImage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Long feedId = feedService.createFeed(command);

        // then
        assertThat(feedId).isEqualTo(1L);

        // Feed 저장 내용 검증
        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository, times(1)).save(feedCaptor.capture());

        Feed capturedFeed = feedCaptor.getValue();
        assertThat(capturedFeed.getUser()).isEqualTo(user);
        assertThat(capturedFeed.getContent()).isEqualTo("내용입니다"); // trim 반영
        assertThat(capturedFeed.getPrice()).isEqualTo(10000L);
        assertThat(capturedFeed.getCategory()).isEqualTo(FeedCategory.FOOD);
        assertThat(capturedFeed.getImageWidth()).isEqualTo(1080);
        assertThat(capturedFeed.getImageHeight()).isEqualTo(720);

        // FeedImage 저장 내용 검증
        ArgumentCaptor<FeedImage> imageCaptor = ArgumentCaptor.forClass(FeedImage.class);
        verify(feedImageRepository, times(1)).save(imageCaptor.capture());

        FeedImage capturedImage = imageCaptor.getValue();
        assertThat(capturedImage.getFeed()).isEqualTo(savedFeedSpy);
        assertThat(capturedImage.getS3ObjectKey()).isEqualTo("feeds/abc.jpg"); // trim 반영

        verifyNoMoreInteractions(feedRepository, feedImageRepository);
    }

    @Test
    @DisplayName("피드 생성 실패 - content 100자 초과면 FEED_CONTENT_TOO_LONG")
    void createFeed_contentTooLong_throws() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        String longContent = "a".repeat(101);
        FeedCreateCommand command = new FeedCreateCommand(
                user,
                longContent,
                10000L,
                FeedCategory.FOOD,
                100,
                100,
                "feeds/abc.jpg"
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_CONTENT_TOO_LONG);

        verifyNoInteractions(feedRepository, feedImageRepository);
    }

    @Test
    @DisplayName("피드 생성 실패 - s3ObjectKey가 null이면 FEED_IMAGE_REQUIRED")
    void createFeed_s3KeyNull_throws() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        FeedCreateCommand command = new FeedCreateCommand(
                user,
                "내용",
                10000L,
                FeedCategory.FOOD,
                100,
                100,
                null
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_REQUIRED);

        verifyNoInteractions(feedRepository, feedImageRepository);
    }

    @Test
    @DisplayName("피드 생성 실패 - s3ObjectKey가 blank이면 FEED_IMAGE_REQUIRED")
    void createFeed_s3KeyBlank_throws() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        FeedCreateCommand command = new FeedCreateCommand(
                user,
                "내용",
                10000L,
                FeedCategory.FOOD,
                100,
                100,
                "   "
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_REQUIRED);

        verifyNoInteractions(feedRepository, feedImageRepository);
    }

    @Test
    @DisplayName("피드 생성 성공 - content가 null이면 빈 문자열로 저장(정책상 허용되는 현재 구현)")
    void createFeed_nullContent_savedAsEmptyString() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        FeedCreateCommand command = new FeedCreateCommand(
                user,
                null,
                10000L,
                FeedCategory.FOOD,
                100,
                100,
                "feeds/abc.jpg"
        );

        Feed savedFeed = spy(Feed.builder()
                .user(user)
                .content("")
                .price(10000L)
                .category(FeedCategory.FOOD)
                .imageWidth(100)
                .imageHeight(100)
                .build());
        given(savedFeed.getId()).willReturn(10L);

        given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);
        given(feedImageRepository.save(any(FeedImage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Long feedId = feedService.createFeed(command);

        // then
        assertThat(feedId).isEqualTo(10L);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository).save(feedCaptor.capture());
        assertThat(feedCaptor.getValue().getContent()).isEqualTo("");

        verify(feedImageRepository).save(any(FeedImage.class));
    }
}
