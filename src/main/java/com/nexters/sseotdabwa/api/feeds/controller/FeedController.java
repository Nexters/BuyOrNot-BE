package com.nexters.sseotdabwa.api.feeds.controller;

import com.nexters.sseotdabwa.api.feeds.facade.FeedFacade;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController implements FeedControllerSpec {

    private final FeedFacade feedFacade;
}
