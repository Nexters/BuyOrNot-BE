package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.feeds.facade.FeedFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController implements FeedControllerSpec {

    private final FeedFacade feedFacade;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeedCreateResponse> createFeed(
            @CurrentUser User user,
            @Valid @RequestBody FeedCreateRequest request
    ) {
        FeedCreateResponse response = feedFacade.createFeed(user, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ApiResponse<List<FeedResponse>> getFeedList(
            @CurrentUser User user
    ) {
        List<FeedResponse> response = feedFacade.getFeedList(user);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{feedId}")
    public ApiResponse<Void> deleteFeed(@CurrentUser User user, @PathVariable Long feedId) {
        feedFacade.deleteFeed(user, feedId);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @PostMapping("/{feedId}/report")
    public ApiResponse<Void> reportFeed(@CurrentUser User user, @PathVariable Long feedId) {
        feedFacade.reportFeed(user, feedId);
        return ApiResponse.success(HttpStatus.OK);
    }
}
