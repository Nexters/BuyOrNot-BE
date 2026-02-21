package com.nexters.sseotdabwa.domain.feeds.scheduler;

import java.util.List;

import com.nexters.sseotdabwa.api.notifications.facade.NotificationFacade;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedSchedulerTest {

    @Mock
    private FeedService feedService;

    @Mock
    private NotificationFacade notificationFacade;

    @InjectMocks
    private FeedScheduler feedScheduler;

    @Test
    @DisplayName("스케줄러가 만료 피드를 마감시키고, 마감된 feedId들로 알림 파이프라인을 호출한다")
    void closeExpiredFeeds_closesAndNotifies() {
        // given
        List<Long> closedIds = List.of(1L, 2L, 3L);
        when(feedService.closeExpiredFeedsAndReturnIds()).thenReturn(closedIds);

        // when
        feedScheduler.closeExpiredFeeds();

        // then
        verify(feedService).closeExpiredFeedsAndReturnIds();
        verify(notificationFacade).onFeedsClosed(closedIds);
    }

    @Test
    @DisplayName("마감된 피드가 없으면 알림 파이프라인을 호출하지 않는다")
    void closeExpiredFeeds_empty_doesNotNotify() {
        // given
        when(feedService.closeExpiredFeedsAndReturnIds()).thenReturn(List.of());

        // when
        feedScheduler.closeExpiredFeeds();

        // then
        verify(feedService).closeExpiredFeedsAndReturnIds();
        verify(notificationFacade, never()).onFeedsClosed(anyList());
    }
}
