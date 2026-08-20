package com.nexters.sseotdabwa.domain.notifications.scheduler;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.notifications.entity.PushLog;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.repository.PushLogRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class MarketingPushSchedulerTest {

    @Autowired private MarketingPushScheduler marketingPushScheduler;
    @Autowired private UserRepository userRepository;
    @Autowired private FeedRepository feedRepository;
    @Autowired private PushLogRepository pushLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean private FcmSender fcmSender;

    private static final int DEFAULT_SIGNUP_DAYS_AGO = 10;

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
                eq("장바구니에 넣어놓고 고민만 하고 있죠?"),
                eq("고민되는 아이템을 업로드해보세요!"),
                argThat(map -> "MARKETING_NO_VOTE".equals(map.get("type")))
        );

        // 발송 성공 로그도 기록된다
        List<PushLog> logs = pushLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).isSuccess()).isTrue();
        assertThat(logs.get(0).getType()).isEqualTo(NotificationType.MARKETING_NO_VOTE);
    }

    @Test
    @DisplayName("FCM 전송이 실패하면 발송 실패 로그가 남는다")
    void sendMarketingPush_recordsFailureLog_whenFcmThrows() throws Exception {
        // given
        User target = createUserWithToken("target");
        setLastOpenedAt(target, LocalDateTime.now().minusDays(3));
        userRepository.save(target);

        doThrow(new RuntimeException("FCM server error"))
                .when(fcmSender).send(anyString(), anyString(), anyString(), anyMap());

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        List<PushLog> logs = pushLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).isSuccess()).isFalse();
        assertThat(logs.get(0).getErrorMessage()).isEqualTo("FCM server error");
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

    @Test
    @DisplayName("가입한지 2일 이내인 유저는 온보딩 푸시가 전담하므로 일반 마케팅 푸시 대상에서 제외된다")
    void sendMarketingPush_excludesRecentlySignedUpUsers() throws Exception {
        // given - 방금 가입한 유저 (createdAt 백데이트 없이 그대로 사용)
        User freshUser = User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("fresh_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .build();
        freshUser.updateFcmToken("fcm_token_fresh");
        userRepository.save(freshUser);
        setLastOpenedAt(freshUser, LocalDateTime.now().minusDays(1));
        userRepository.save(freshUser);

        // when
        marketingPushScheduler.sendMarketingPushWednesday();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    // ===== sendInactive3DayPush / sendInactive7DayPush =====

    @Test
    @DisplayName("마지막 피드 등록일이 정확히 3일 전이면 3일 재참여 유도 푸시가 발송된다")
    void sendInactive3DayPush_sendsToExactly3DaysInactiveUsers() {
        // given
        User user = createUserWithToken("inactive3");
        createFeedWithCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(3).atTime(12, 0));

        // when
        marketingPushScheduler.sendInactive3DayPush();

        // then
        verify(fcmSender, times(1)).send(
                eq("fcm_token_inactive3"),
                eq("3일째 조용하시네요..?!"),
                eq("고민되는 물건 살까말까에서 같이 정해봐요🤔"),
                argThat(map -> "MARKETING_INACTIVE_3D".equals(map.get("type")) && "FEED_CREATE".equals(map.get("screen")))
        );
    }

    @Test
    @DisplayName("마지막 피드 등록일이 2일/4일 전이면 3일 재참여 유도 푸시 대상에서 제외된다")
    void sendInactive3DayPush_excludesUsersOutsideExactWindow() {
        // given
        User twoDaysAgo = createUserWithToken("twodays");
        createFeedWithCreatedAt(twoDaysAgo, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2).atTime(12, 0));

        User fourDaysAgo = createUserWithToken("fourdays");
        createFeedWithCreatedAt(fourDaysAgo, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(4).atTime(12, 0));

        // when
        marketingPushScheduler.sendInactive3DayPush();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("가장 최근 피드 기준으로 판단한다 (재활동한 유저는 대상에서 제외)")
    void sendInactive3DayPush_excludesUsersWithMoreRecentFeed() {
        // given - 3일 전에도 피드를 올렸지만, 그 이후 다시 활동한 유저
        User activeAgain = createUserWithToken("activeagain");
        ZoneId kst = ZoneId.of("Asia/Seoul");
        createFeedWithCreatedAt(activeAgain, LocalDate.now(kst).minusDays(3).atTime(12, 0));
        createFeedWithCreatedAt(activeAgain, LocalDate.now(kst).minusDays(1).atTime(12, 0));

        // when
        marketingPushScheduler.sendInactive3DayPush();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("pushEnabled=false 유저는 3일 재참여 유도 푸시 대상에서 제외된다")
    void sendInactive3DayPush_excludesPushDisabledUsers() throws Exception {
        // given
        User user = createUserWithToken("disabled3");
        setPushEnabled(user, false);
        userRepository.save(user);
        createFeedWithCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(3).atTime(12, 0));

        // when
        marketingPushScheduler.sendInactive3DayPush();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("마지막 피드 등록일이 정확히 7일 전이면 7일 재참여 유도 푸시가 발송된다")
    void sendInactive7DayPush_sendsToExactly7DaysInactiveUsers() {
        // given
        User user = createUserWithToken("inactive7");
        createFeedWithCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(7).atTime(12, 0));

        // when
        marketingPushScheduler.sendInactive7DayPush();

        // then
        verify(fcmSender, times(1)).send(
                eq("fcm_token_inactive7"),
                eq("일주일째 못 봤네요, 잘 지내고 있어요?"),
                eq("그동안 고민되는 거 하나쯤 있었을 텐데.."),
                argThat(map -> "MARKETING_INACTIVE_7D".equals(map.get("type")))
        );
    }

    // ===== sendOnboardingPush =====

    @Test
    @DisplayName("가입 후 정확히 2일 경과 + 미업로드 유저에게 온보딩 유도 푸시가 발송된다")
    void sendOnboardingPush_sendsToEligibleUsers() {
        // given
        User user = createUserWithToken("onboarding");
        setCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2).atTime(12, 0));

        // when
        marketingPushScheduler.sendOnboardingPush();

        // then
        verify(fcmSender, times(1)).send(
                eq("fcm_token_onboarding"),
                eq("살까말까 가입해주셔서 감사해요!"),
                eq("이제 고민되는 거 하나만 올리면 시작이에요!"),
                argThat(map -> "MARKETING_ONBOARDING_INACTIVE".equals(map.get("type")) && "FEED_CREATE".equals(map.get("screen")))
        );
    }

    @Test
    @DisplayName("피드를 이미 등록한 유저는 온보딩 유도 푸시 대상에서 제외된다")
    void sendOnboardingPush_excludesUsersWithFeeds() {
        // given
        User user = createUserWithToken("onboardinghasfeed");
        setCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2).atTime(12, 0));
        createFeedWithCreatedAt(user, LocalDateTime.now());

        // when
        marketingPushScheduler.sendOnboardingPush();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("가입한지 1일밖에 안 된 유저는 온보딩 유도 푸시 대상에서 제외된다 (아직 2일 안 지남)")
    void sendOnboardingPush_excludesTooRecentUsers() {
        // given
        User user = createUserWithToken("toorecent");
        setCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1).atTime(12, 0));

        // when
        marketingPushScheduler.sendOnboardingPush();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("pushEnabled=false 유저는 온보딩 유도 푸시 대상에서 제외된다")
    void sendOnboardingPush_excludesPushDisabledUsers() throws Exception {
        // given
        User user = createUserWithToken("onboardingdisabled");
        setCreatedAt(user, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2).atTime(12, 0));
        setPushEnabled(user, false);
        userRepository.save(user);

        // when
        marketingPushScheduler.sendOnboardingPush();

        // then
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    // ---------------- helpers ----------------

    /**
     * 가입일이 최근인 유저는 온보딩 유도 푸시 대상 기간과 겹치므로,
     * 별도로 가입일을 지정하지 않는 테스트는 충분히 오래 전에 가입한 것으로 처리한다.
     */
    private User createUser(String prefix) {
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(prefix + "_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .build());
        setCreatedAt(user, LocalDateTime.now().minusDays(DEFAULT_SIGNUP_DAYS_AGO));
        return user;
    }

    private User createUserWithToken(String prefix) {
        User user = User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(prefix + "_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .build();
        user.updateFcmToken("fcm_token_" + prefix);
        User saved = userRepository.save(user);
        setCreatedAt(saved, LocalDateTime.now().minusDays(DEFAULT_SIGNUP_DAYS_AGO));
        return saved;
    }

    private Feed createFeedWithCreatedAt(User user, LocalDateTime createdAt) {
        Feed feed = feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build());
        entityManager.createQuery("UPDATE Feed f SET f.createdAt = :value WHERE f.id = :id")
                .setParameter("value", createdAt)
                .setParameter("id", feed.getId())
                .executeUpdate();
        return feed;
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

    /**
     * createdAt은 @Column(updatable = false)라 엔티티 저장/merge로는 바꿀 수 없어
     * 벌크 JPQL update로 직접 DB 값을 변경한다 (가입일 기준 테스트를 위한 용도).
     */
    private void setCreatedAt(User user, LocalDateTime value) {
        entityManager.createQuery("UPDATE User u SET u.createdAt = :value WHERE u.id = :id")
                .setParameter("value", value)
                .setParameter("id", user.getId())
                .executeUpdate();
    }
}
