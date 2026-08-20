package com.nexters.sseotdabwa.domain.notifications.scheduler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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

    private static final String MARKETING_TITLE = "살까, 말까 고민되는 거 있어요?";
    private static final String MARKETING_BODY = "고민되는 아이템을 업로드해보세요!";
    private static final int RECENT_DAYS = 7;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserService userService;
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

    private void sendMarketingPush() {
        LocalDateTime cutoff = LocalDateTime.now(KST).minusDays(RECENT_DAYS);
        List<User> targets = userService.findMarketingTargets(cutoff);

        if (targets.isEmpty()) {
            log.info("[마케팅 푸시] 발송 대상 없음");
            return;
        }

        log.info("[마케팅 푸시] 발송 시작. 대상 {}명", targets.size());

        int successCount = 0;
        for (User user : targets) {
            try {
                Map<String, String> data = Map.of("type", NotificationType.MARKETING_NO_VOTE.name());
                fcmSender.send(user.getFcmToken(), MARKETING_TITLE, MARKETING_BODY, data);
                successCount++;
                pushLogService.record(user.getId(), null, NotificationType.MARKETING_NO_VOTE,
                        MARKETING_TITLE, MARKETING_BODY, true, null);
            } catch (Exception e) {
                log.warn("[마케팅 푸시] FCM 전송 실패. userId={}", user.getId(), e);
                pushLogService.record(user.getId(), null, NotificationType.MARKETING_NO_VOTE,
                        MARKETING_TITLE, MARKETING_BODY, false, e.getMessage());
            }
        }

        log.info("[마케팅 푸시] 발송 완료. 성공={}/전체={}", successCount, targets.size());
    }
}
