package com.nexters.sseotdabwa.api.feeds.facade;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedCreateCommand;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class FeedFacade {

    private final FeedService feedService;

    public FeedCreateResponse createFeed(User user, FeedCreateRequest request) {
        Long feedId = feedService.createFeed(
                new FeedCreateCommand(
                    user,
                    request.content(),
                    request.price(),
                    request.category(),
                    request.imageWidth(),
                    request.imageHeight(),
                    request.s3ObjectKey()
                )
        );
        return FeedCreateResponse.of(feedId);
    }
}
