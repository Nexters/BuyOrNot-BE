package com.nexters.sseotdabwa.domain.feeds.scheduler;

import java.util.List;

import com.nexters.sseotdabwa.api.notifications.facade.NotificationFacade;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedScheduler {

    private final FeedService feedService;
    private final NotificationFacade notificationFacade;

    /**
     * 10분마다 만료된 피드를 마감하고, 마감된 피드에 대한 알림을 생성한다.
     *
     * 흐름:
     * - 만료 OPEN 피드 feedId 조회
     * - bulk CLOSED
     * - feedId 기반 알림 생성 + 푸시
     */
    @Scheduled(fixedRate = 600_000)
    public void closeExpiredFeeds() {
        List<Long> closedFeedIds = feedService.closeExpiredFeedsAndReturnIds();
        if (closedFeedIds.isEmpty()) {
            log.info("만료 피드 없음");
            return;
        }

        log.info("만료 피드 {} 건 마감 처리 완료. feedIds={}", closedFeedIds.size(), closedFeedIds);

        // 알림 생성/푸시는 내부에서 처리
        notificationFacade.onFeedsClosed(closedFeedIds);
    }
}
