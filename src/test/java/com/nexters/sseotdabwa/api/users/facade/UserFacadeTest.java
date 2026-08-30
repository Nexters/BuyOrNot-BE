package com.nexters.sseotdabwa.api.users.facade;

import java.util.UUID;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class UserFacadeTest {

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("알림이 전혀 없는 유저는 탈퇴에 성공한다")
    void withdraw_withoutNotifications_succeeds() {
        // given
        User user = createUser();

        // when
        userFacade.withdraw(user);

        // then
        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    @DisplayName("본인 피드에 알림이 달린 유저도 탈퇴에 성공하고 해당 알림이 삭제된다")
    void withdraw_withNotificationOnOwnFeed_succeeds() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);
        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .feed(feed)
                .type(NotificationType.MY_FEED_CLOSED)
                .title("투표 종료!")
                .body("test")
                .build());

        // when
        userFacade.withdraw(user);

        // then
        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(feedRepository.existsById(feed.getId())).isFalse();
        assertThat(notificationRepository.existsById(notification.getId())).isFalse();
    }

    @Test
    @DisplayName("남의 피드에 투표해서 받은 알림이 있는 유저도 탈퇴에 성공하고 해당 알림이 삭제된다")
    void withdraw_withNotificationAsParticipant_succeeds() {
        // given
        User author = createUser();
        User participant = createUser();
        Feed authorsFeed = createFeed(author);

        Notification participantNotification = notificationRepository.save(Notification.builder()
                .user(participant)
                .feed(authorsFeed)
                .type(NotificationType.PARTICIPATED_FEED_CLOSED)
                .title("투표 종료!")
                .body("test")
                .build());

        // when
        userFacade.withdraw(participant);

        // then
        assertThat(userRepository.existsById(participant.getId())).isFalse();
        assertThat(notificationRepository.existsById(participantNotification.getId())).isFalse();
        // 참여자 탈퇴가 작성자의 피드에는 영향을 주면 안 됨
        assertThat(feedRepository.existsById(authorsFeed.getId())).isTrue();
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
