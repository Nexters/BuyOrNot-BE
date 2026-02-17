package com.nexters.sseotdabwa.domain.feeds.scheduler;

import com.nexters.sseotdabwa.domain.feeds.service.FeedService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedSchedulerTest {

    @Mock
    private FeedService feedService;

    @InjectMocks
    private FeedScheduler feedScheduler;

    @Test
    @DisplayName("스케줄러가 FeedService.closeExpiredFeeds()를 호출한다")
    void closeExpiredFeeds_delegatesToFeedService() {
        // given
        when(feedService.closeExpiredFeeds()).thenReturn(3);

        // when
        feedScheduler.closeExpiredFeeds();

        // then
        verify(feedService).closeExpiredFeeds();
    }
}
