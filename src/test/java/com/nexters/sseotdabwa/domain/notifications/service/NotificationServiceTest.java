package com.nexters.sseotdabwa.domain.notifications.service;

import java.util.UUID;

import com.nexters.sseotdabwa.common.exception.GlobalException;
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
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Test
    @DisplayName("알림 읽음 처리 성공 - 본인 알림이면 isRead=true")
    void markAsRead_success() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        Notification noti = notificationRepository.save(Notification.builder()
                .user(user)
                .feed(feed)
                .type(NotificationType.MY_FEED_CLOSED)
                .title("투표 종료!")
                .body("test")
                .build());

        // when
        notificationService.markAsRead(user.getId(), noti.getId());

        // then
        Notification updated = notificationRepository.findById(noti.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 타인 알림이면 NOTIFICATION_NOT_FOUND")
    void markAsRead_otherUsersNotification_throwsNotFound() {
        // given
        User owner = createUser();
        User other = createUser();
        Feed feed = createFeed(owner);

        Notification noti = notificationRepository.save(Notification.builder()
                .user(owner)
                .feed(feed)
                .type(NotificationType.MY_FEED_CLOSED)
                .title("투표 종료!")
                .body("test")
                .build());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(other.getId(), noti.getId()))
                .isInstanceOf(GlobalException.class)
                // 메시지는 프로젝트 에러메시지에 맞춰져있을 것
                .hasMessageContaining("알림을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("중복 방지 - (user, feed, type) 이미 존재하면 createIfAbsent는 null 반환")
    void createIfAbsent_duplicate_returnsNull() {
        // given
        User user = createUser();
        Feed feed = createFeed(user);

        notificationService.createIfAbsent(user, feed, NotificationType.MY_FEED_CLOSED, "t", "b");

        // when
        Notification second = notificationService.createIfAbsent(user, feed, NotificationType.MY_FEED_CLOSED, "t", "b");

        // then
        assertThat(second).isNull();
        assertThat(notificationRepository.count()).isEqualTo(1);
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
