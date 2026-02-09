package com.nexters.sseotdabwa.api.feeds.controller;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
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
    public ApiResponse<FeedCreateResponse> createFeed(
            @CurrentUser User user,
            @Valid @RequestBody FeedCreateRequest request
    ) {
        FeedCreateResponse response = feedFacade.createFeed(user, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }
}
