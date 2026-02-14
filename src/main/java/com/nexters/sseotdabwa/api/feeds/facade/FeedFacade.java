package com.nexters.sseotdabwa.api.feeds.facade;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feed 생성 흐름 조합 Facade
 * - FeedService(Feed 저장) + FeedImageService(이미지 저장) 조합
 */
@Component
@RequiredArgsConstructor
public class FeedFacade {

    private final FeedService feedService;
    private final FeedImageService feedImageService;

    /**
     * 피드 생성 + 피드 이미지 저장
     */
    @Transactional
    public FeedCreateResponse createFeed(User user, FeedCreateRequest request) {
        FeedCreateCommand command = new FeedCreateCommand(
                user,
                request.content(),
                request.price(),
                request.category(),
                request.imageWidth(),
                request.imageHeight(),
                request.s3ObjectKey()
        );

        // 1) Feed 저장
        Feed savedFeed = feedService.createFeed(command);

        // 2) FeedImage 저장
        feedImageService.save(savedFeed, command.s3ObjectKey());

        return new FeedCreateResponse(savedFeed.getId());
    }
}
