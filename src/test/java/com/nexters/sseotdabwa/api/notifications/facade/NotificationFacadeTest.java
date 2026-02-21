package com.nexters.sseotdabwa.api.notifications.facade;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;

import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;

import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class NotificationFacadeTest {

    @Autowired private NotificationFacade notificationFacade;

    @Autowired private UserRepository userRepository;
    @Autowired private FeedRepository feedRepository;
    @Autowired private FeedImageRepository feedImageRepository;
    @Autowired private VoteLogRepository voteLogRepository;
    @Autowired private NotificationRepository notificationRepository;

    @MockBean
    private FcmSender fcmSender;

    @Test
    @DisplayName("피드 마감 처리 시 작성자/참여자 알림이 생성된다")
    void onFeedsClosed_createsNotifications_forAuthorAndParticipants() {
        // given
        User author = createUser("author");
        User voter1 = createUser("voter1");
        User voter2 = createUser("voter2");

        Feed feed = createFeed(author);
        createFeedImage(feed);

        // voter 2명 투표 로그
        voteLogRepository.save(VoteLog.builder()
                .user(voter1)
                .feed(feed)
                .choice(VoteChoice.YES)
                .voteType(VoteType.USER)
                .build());

        voteLogRepository.save(VoteLog.builder()
                .user(voter2)
                .feed(feed)
                .choice(VoteChoice.NO)
                .voteType(VoteType.USER)
                .build());

        // when
        notificationFacade.onFeedsClosed(List.of(feed.getId()));

        // then
        // 작성자 1 + 참여자 2 = 3개 생성 기대
        assertThat(notificationRepository.count()).isEqualTo(3);

        assertThat(notificationRepository.existsByUserIdAndFeedIdAndType(
                author.getId(), feed.getId(), NotificationType.MY_FEED_CLOSED)).isTrue();

        assertThat(notificationRepository.existsByUserIdAndFeedIdAndType(
                voter1.getId(), feed.getId(), NotificationType.PARTICIPATED_FEED_CLOSED)).isTrue();

        assertThat(notificationRepository.existsByUserIdAndFeedIdAndType(
                voter2.getId(), feed.getId(), NotificationType.PARTICIPATED_FEED_CLOSED)).isTrue();
    }

    @Test
    @DisplayName("pushEnabled=true + fcmToken 존재하면 FCM send가 호출된다")
    void onFeedsClosed_sendsFcm_whenEligible() {
        // given
        User author = createUser("author");
        author.updateFcmToken("fcm_token_test"); // canReceivePush() 만족 (pushEnabled 기본 true)
        userRepository.save(author);

        Feed feed = createFeed(author);
        createFeedImage(feed);

        // when
        notificationFacade.onFeedsClosed(List.of(feed.getId()));

        // then
        // 최소 1회 이상 전송 시도
        verify(fcmSender, atLeastOnce()).send(
                eq("fcm_token_test"),
                anyString(),  // title
                anyString(),  // body
                anyMap()      // data payload
        );
    }

    @Test
    @DisplayName("pushEnabled=false 이면 알림은 저장되지만 FCM send는 호출되지 않는다")
    void onFeedsClosed_doesNotSendFcm_whenPushDisabled() throws Exception {
        // given
        User author = createUser("author");
        author.updateFcmToken("fcm_token_test");

        // pushEnabled=false로 강제 설정 (User에 disable 메서드가 없으므로 테스트에서만 리플렉션 사용)
        setPushEnabled(author, false);
        userRepository.save(author);

        // sanity check
        assertThat(author.canReceivePush()).isFalse();

        Feed feed = createFeed(author);
        createFeedImage(feed);

        // when
        notificationFacade.onFeedsClosed(List.of(feed.getId()));

        // then
        // 알림 저장은 됨 (작성자 알림 1개)
        assertThat(notificationRepository.count()).isEqualTo(1);

        // 전송은 안됨
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("fcmToken이 blank/null이면 알림은 저장되지만 FCM send는 호출되지 않는다")
    void onFeedsClosed_doesNotSendFcm_whenTokenMissing() {
        // given
        User author = createUser("author");
        author.updateFcmToken("   "); // blank -> canReceivePush false
        userRepository.save(author);

        Feed feed = createFeed(author);
        createFeedImage(feed);

        // when
        notificationFacade.onFeedsClosed(List.of(feed.getId()));

        // then
        assertThat(notificationRepository.count()).isEqualTo(1);
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    // ---------------- helpers ----------------

    private User createUser(String prefix) {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(prefix + "_" + UUID.randomUUID().toString().substring(0, 6))
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

    private void createFeedImage(Feed feed) {
        feedImageRepository.save(FeedImage.builder()
                .feed(feed)
                .s3ObjectKey("feeds/test_" + UUID.randomUUID() + ".jpg")
                .build());
    }

    private void setPushEnabled(User user, boolean value) throws Exception {
        Field field = User.class.getDeclaredField("pushEnabled");
        field.setAccessible(true);
        field.set(user, value);
    }
}
