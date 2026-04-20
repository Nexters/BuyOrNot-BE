package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequestV2;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponseV2;
import com.nexters.sseotdabwa.api.feeds.facade.FeedFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/feeds")
@RequiredArgsConstructor
public class FeedControllerV2 implements FeedControllerSpecV2 {

    private final FeedFacade feedFacade;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeedCreateResponse> createFeed(
            @CurrentUser User user,
            @Valid @RequestBody FeedCreateRequestV2 request
    ) {
        FeedCreateResponse response = feedFacade.createFeedV2(user, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ApiResponse<CursorPageResponse<FeedResponseV2>> getFeedList(
            @CurrentUser User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) FeedStatus feedStatus,
            @RequestParam(name = "category", required = false) List<FeedCategory> categories
    ) {
        CursorPageResponse<FeedResponseV2> response = feedFacade.getFeedListV2(user, cursor, size, feedStatus, categories);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @GetMapping("/{feedId}")
    public ApiResponse<FeedResponseV2> getFeedDetail(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        FeedResponseV2 response = feedFacade.getFeedDetailV2(user, feedId);
        return ApiResponse.success(response, HttpStatus.OK);
    }
}
