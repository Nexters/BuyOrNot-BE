package com.nexters.sseotdabwa.domain.notifications.scheduler;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class MarketingPushSchedulerTest {

    @Autowired private MarketingPushScheduler marketingPushScheduler;
    @Autowired private UserRepository userRepository;
    @Autowired private FeedRepository feedRepository;

    @MockBean private FcmSender fcmSender;

    @Test
    @DisplayName("투표 미등록 + 최근 7일 앱 오픈 유저에게 마케팅 푸시 발송")
    void sendMarketingPush_sendsToEligibleUsers() throws Exception {
        // given
        User target = createUserWithToken("target");
        setLastOpenedAt(target, LocalDateTime.now().minusDays(3));
        userRepository.save(target);

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, times(1)).send(
                eq("fcm_token_target"),
                eq("살까, 말까 고민되는 거 있어요?"),
                eq("고민되는 아이템을 업로드해보세요!"),
                anyMap()
        );
    }

    @Test
    @DisplayName("7일 초과 비활성 유저는 발송 제외")
    void sendMarketingPush_excludesInactiveUsers() throws Exception {
        // given
        User inactive = createUserWithToken("inactive");
        setLastOpenedAt(inactive, LocalDateTime.now().minusDays(8));
        userRepository.save(inactive);

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("lastOpenedAt이 null인 유저는 발송 제외")
    void sendMarketingPush_excludesUsersWithNullLastOpenedAt() {
        // given
        User user = createUserWithToken("nullopen");
        // lastOpenedAt = null (기본값)
        userRepository.save(user);

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("피드를 등록한 유저는 발송 제외")
    void sendMarketingPush_excludesUsersWithFeeds() throws Exception {
        // given
        User userWithFeed = createUserWithToken("hasfeed");
        setLastOpenedAt(userWithFeed, LocalDateTime.now().minusDays(1));
        userRepository.save(userWithFeed);

        feedRepository.save(Feed.builder()
                .user(userWithFeed)
                .content("이미 투표 등록함")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build());

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("pushEnabled=false 유저는 발송 제외")
    void sendMarketingPush_excludesPushDisabledUsers() throws Exception {
        // given
        User user = createUserWithToken("pushdisabled");
        setLastOpenedAt(user, LocalDateTime.now().minusDays(1));
        setPushEnabled(user, false);
        userRepository.save(user);

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("fcmToken이 없는 유저는 발송 제외")
    void sendMarketingPush_excludesUsersWithoutToken() throws Exception {
        // given
        User user = createUser("notoken");
        setLastOpenedAt(user, LocalDateTime.now().minusDays(1));
        // fcmToken = null
        userRepository.save(user);

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("발송 대상 다수 - 각 유저에게 개별 발송")
    void sendMarketingPush_sendsToMultipleTargets() throws Exception {
        // given
        for (int i = 0; i < 3; i++) {
            User user = createUserWithToken("multi" + i);
            setLastOpenedAt(user, LocalDateTime.now().minusDays(1));
            userRepository.save(user);
        }

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, times(3)).send(anyString(), anyString(), anyString(), anyMap());
    }

    // ---------------- helpers ----------------

    private User createUser(String prefix) {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(prefix + "_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private User createUserWithToken(String prefix) {
        User user = User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(prefix + "_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .build();
        user.updateFcmToken("fcm_token_" + prefix);
        return userRepository.save(user);
    }

    private void setLastOpenedAt(User user, LocalDateTime value) throws Exception {
        Field field = User.class.getDeclaredField("lastOpenedAt");
        field.setAccessible(true);
        field.set(user, value);
    }

    private void setPushEnabled(User user, boolean value) throws Exception {
        Field field = User.class.getDeclaredField("pushEnabled");
        field.setAccessible(true);
        field.set(user, value);
    }
}
