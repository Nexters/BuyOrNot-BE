package com.nexters.sseotdabwa.api.feeds.facade;

import com.nexters.sseotdabwa.domain.feeds.service.FeedService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedFacade {

    private final FeedService feedService;
}
