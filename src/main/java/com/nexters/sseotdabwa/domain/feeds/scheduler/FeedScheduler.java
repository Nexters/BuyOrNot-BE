package com.nexters.sseotdabwa.domain.feeds.scheduler;

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

    @Scheduled(fixedRate = 600_000)
    public void closeExpiredFeeds() {
        int closedCount = feedService.closeExpiredFeeds();
        if (closedCount > 0) {
            log.info("만료 피드 {} 건 마감 처리 완료", closedCount);
        }
    }
}
