package com.nexters.sseotdabwa.domain.feeds.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

@ExtendWith(MockitoExtension.class)
class FeedServiceUnitTest {

    @Mock
    private FeedRepository feedRepository;

    @InjectMocks
    private FeedService feedService;

    @Test
    @DisplayName("피드 생성 성공 - Feed 저장 후 반환")
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
                List.of("feeds/abc.jpg"),
                null,
                null
        );

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

        // when
        Feed result = feedService.createFeed(command);

        // then
        assertThat(result.getId()).isEqualTo(1L);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository, times(1)).save(feedCaptor.capture());

        Feed capturedFeed = feedCaptor.getValue();
        assertThat(capturedFeed.getContent()).isEqualTo("내용입니다");
        assertThat(capturedFeed.getPrice()).isEqualTo(10000L);

        verifyNoMoreInteractions(feedRepository);
    }

    @Test
    @DisplayName("피드 생성 실패 - 이미지가 없으면(Empty List) FEED_IMAGE_REQUIRED")
    void createFeed_imageEmpty_throws() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        FeedCreateCommand command = new FeedCreateCommand(
                user,
                "내용",
                10000L,
                FeedCategory.FOOD,
                100,
                100,
                Collections.emptyList(),
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_REQUIRED);

        verifyNoInteractions(feedRepository);
    }

    @Test
    @DisplayName("피드 생성 실패 - s3ObjectKeys 리스트 내부 원소에 null이나 공백이 포함되면 FEED_IMAGE_REQUIRED")
    void createFeed_s3KeyListContainsInvalidElement_throws() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        // 1. null 원소가 포함된 경우
        FeedCreateCommand commandWithNull = new FeedCreateCommand(
                user, "내용", 10000L, FeedCategory.FOOD, 100, 100,
                Collections.singletonList(null), null, null
        );

        // 2. 공백 원소가 포함된 경우
        FeedCreateCommand commandWithBlank = new FeedCreateCommand(
                user, "내용", 10000L, FeedCategory.FOOD, 100, 100,
                List.of("  "), null, null
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(commandWithNull))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_REQUIRED);

        assertThatThrownBy(() -> feedService.createFeed(commandWithBlank))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_REQUIRED);

        verifyNoInteractions(feedRepository);
    }

    @Test
    @DisplayName("피드 생성 실패 - 이미지 3개 초과 시 FEED_IMAGE_LIMIT_EXCEEDED")
    void createFeed_imageLimitExceeded_throws() {
        // given
        User user = User.builder().socialId("x").nickname("n").build();

        FeedCreateCommand command = new FeedCreateCommand(
                user,
                "내용",
                10000L,
                FeedCategory.FOOD,
                100,
                100,
                List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg"), // 4개 전달
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_LIMIT_EXCEEDED);

        verifyNoInteractions(feedRepository);
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
                List.of("feeds/1.jpg", "feeds/2.jpg"),
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_CONTENT_TOO_LONG);

        verifyNoInteractions(feedRepository);
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
                null,
                null,
                null
        );

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(command))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_IMAGE_REQUIRED);

        verifyNoInteractions(feedRepository);
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
                List.of("feeds/1.jpg", "feeds/2.jpg"),
                null,
                null
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

        // when
        Feed result = feedService.createFeed(command);

        // then
        assertThat(result.getId()).isEqualTo(10L);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository).save(feedCaptor.capture());
        assertThat(feedCaptor.getValue().getContent()).isEqualTo("");

        verifyNoMoreInteractions(feedRepository);
    }

    @Test
    @DisplayName("삭제된 피드 제외 전체 조회 성공")
    void findAllExceptDeleted_success() {
        // given
        User user = User.builder()
                .socialId("test-social-id")
                .nickname("테스트유저")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        Feed feed1 = Feed.builder()
                .user(user)
                .content("피드1")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build();
        Feed feed2 = Feed.builder()
                .user(user)
                .content("피드2")
                .price(20000L)
                .category(FeedCategory.FOOD)
                .imageWidth(300)
                .imageHeight(400)
                .build();
        given(feedRepository.findByReportStatusNotOrderByCreatedAtDesc(ReportStatus.DELETED))
                .willReturn(List.of(feed1, feed2));

        // when
        List<Feed> result = feedService.findAllExceptDeleted();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("피드 단건 조회 성공")
    void findById_success() {
        // given
        User user = User.builder()
                .socialId("test-social-id")
                .nickname("테스트유저")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        Feed feed = Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build();
        given(feedRepository.findById(1L)).willReturn(Optional.of(feed));

        // when
        Feed result = feedService.findById(1L);

        // then
        assertThat(result).isEqualTo(feed);
    }

    @Test
    @DisplayName("존재하지 않는 피드 조회 시 FEED_NOT_FOUND 예외")
    void findById_notFound() {
        // given
        given(feedRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedService.findById(999L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("피드 삭제 성공")
    void delete_success() {
        // given
        User user = User.builder()
                .socialId("test-social-id")
                .nickname("테스트유저")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        Feed feed = Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build();

        // when
        feedService.delete(feed);

        // then
        verify(feedRepository).delete(feed);
    }
}
