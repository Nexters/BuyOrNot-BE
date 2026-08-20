package com.nexters.sseotdabwa.domain.notifications.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.service.PushLogService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingPushScheduler {

    private static final String MARKETING_TITLE = "장바구니에 넣어놓고 고민만 하고 있죠?";
    private static final String MARKETING_BODY = "고민되는 아이템을 업로드해보세요!";

    private static final String INACTIVE_3D_TITLE = "3일째 조용하시네요..?!";
    private static final String INACTIVE_3D_BODY = "고민되는 물건 살까말까에서 같이 정해봐요🤔";

    private static final String INACTIVE_7D_TITLE = "일주일째 못 봤네요, 잘 지내고 있어요?";
    private static final String INACTIVE_7D_BODY = "그동안 고민되는 거 하나쯤 있었을 텐데..";

    private static final String ONBOARDING_TITLE = "살까말까 가입해주셔서 감사해요!";
    private static final String ONBOARDING_BODY = "이제 고민되는 거 하나만 올리면 시작이에요!";

    private static final String FEED_CREATE_SCREEN = "FEED_CREATE";
    private static final String HOME_SCREEN = "HOME";

    private static final int RECENT_DAYS = 7;
    private static final int INACTIVE_3D_DAYS = 3;
    private static final int INACTIVE_7D_DAYS = 7;
    private static final int ONBOARDING_DAYS = 2;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserService userService;
    private final FeedService feedService;
    private final FcmSender fcmSender;
    private final PushLogService pushLogService;

    // 매주 수요일 13:00 (KST)
    @Scheduled(cron = "0 0 13 * * WED", zone = "Asia/Seoul")
    public void sendMarketingPushWednesday() {
        sendMarketingPush();
    }

    // 매주 토요일 13:00 (KST)
    @Scheduled(cron = "0 0 13 * * SAT", zone = "Asia/Seoul")
    public void sendMarketingPushSaturday() {
        sendMarketingPush();
    }

    // 매일 13:00 (KST) - 마지막 피드 등록 후 3일 미활동 재참여 유도
    @Scheduled(cron = "0 0 13 * * *", zone = "Asia/Seoul")
    public void sendInactive3DayPush() {
        sendReengagementPush(INACTIVE_3D_DAYS, NotificationType.MARKETING_INACTIVE_3D, INACTIVE_3D_TITLE, INACTIVE_3D_BODY);
    }

    // 매일 13:00 (KST) - 마지막 피드 등록 후 7일 미활동 재참여 유도
    @Scheduled(cron = "0 0 13 * * *", zone = "Asia/Seoul")
    public void sendInactive7DayPush() {
        sendReengagementPush(INACTIVE_7D_DAYS, NotificationType.MARKETING_INACTIVE_7D, INACTIVE_7D_TITLE, INACTIVE_7D_BODY);
    }

    // 매일 13:00 (KST) - 가입 후 2일 미업로드 온보딩 유도
    @Scheduled(cron = "0 0 13 * * *", zone = "Asia/Seoul")
    public void sendOnboardingPush() {
        LocalDateTime rangeStart = todayRangeStart(ONBOARDING_DAYS);
        LocalDateTime rangeEnd = rangeStart.plusDays(1);

        List<User> targets = userService.findOnboardingTargets(rangeStart, rangeEnd);
        sendToTargets(targets, NotificationType.MARKETING_ONBOARDING_INACTIVE, ONBOARDING_TITLE, ONBOARDING_BODY, "[온보딩 유도 푸시]", FEED_CREATE_SCREEN);
    }

    private void sendMarketingPush() {
        LocalDateTime cutoff = LocalDateTime.now(KST).minusDays(RECENT_DAYS);
        LocalDateTime signupCutoff = LocalDateTime.now(KST).minusDays(ONBOARDING_DAYS);

        List<User> targets = userService.findMarketingTargets(cutoff, signupCutoff);
        sendToTargets(targets, NotificationType.MARKETING_NO_VOTE, MARKETING_TITLE, MARKETING_BODY, "[마케팅 푸시]", HOME_SCREEN);
    }

    /**
     * 마지막 피드 등록일 기준 정확히 inactiveDays일 경과한 유저에게 재참여 유도 푸시 발송
     */
    private void sendReengagementPush(int inactiveDays, NotificationType type, String title, String body) {
        LocalDateTime rangeStart = todayRangeStart(inactiveDays);
        LocalDateTime rangeEnd = rangeStart.plusDays(1);

        List<Long> userIds = feedService.findUserIdsByLastFeedCreatedAtBetween(rangeStart, rangeEnd);
        if (userIds.isEmpty()) {
            log.info("[재참여 유도 푸시] 발송 대상 없음. type={}", type);
            return;
        }

        List<User> targets = userService.findByIds(userIds).stream()
                .filter(User::canReceivePush)
                .toList();

        sendToTargets(targets, type, title, body, "[재참여 유도 푸시]", FEED_CREATE_SCREEN);
    }

    private void sendToTargets(List<User> targets, NotificationType type, String title, String body, String logPrefix, String screen) {
        if (targets.isEmpty()) {
            log.info("{} 발송 대상 없음. type={}", logPrefix, type);
            return;
        }

        log.info("{} 발송 시작. type={}, 대상 {}명", logPrefix, type, targets.size());

        int successCount = 0;
        for (User user : targets) {
            try {
                Map<String, String> data = Map.of("type", type.name(), "screen", screen);
                fcmSender.send(user.getFcmToken(), title, body, data);
                successCount++;
                pushLogService.record(user.getId(), null, type, title, body, true, null);
            } catch (Exception e) {
                log.warn("{} FCM 전송 실패. userId={}, type={}", logPrefix, user.getId(), type, e);
                pushLogService.record(user.getId(), null, type, title, body, false, e.getMessage());
            }
        }

        log.info("{} 발송 완료. type={}, 성공={}/전체={}", logPrefix, type, successCount, targets.size());
    }

    /**
     * 오늘 기준 daysAgo일 전 날짜의 00:00 (KST)
     */
    private LocalDateTime todayRangeStart(int daysAgo) {
        return LocalDate.now(KST).minusDays(daysAgo).atStartOfDay();
    }
}
