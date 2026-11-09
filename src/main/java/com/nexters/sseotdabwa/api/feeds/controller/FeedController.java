package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedAiDraftRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedAiDraftResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequestGuest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedGuestDeleteRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.feeds.facade.FeedAiDraftFacade;
import com.nexters.sseotdabwa.api.feeds.facade.FeedFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.response.StringCursorPageResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.common.util.ClientIpResolver;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController implements FeedControllerSpec {

    private final FeedFacade feedFacade;
    private final FeedAiDraftFacade feedAiDraftFacade;

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
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeedCreateResponse> createGuestFeed(
            @Valid @RequestBody FeedCreateRequestGuest request
    ) {
        FeedCreateResponse response = feedFacade.createGuestFeed(request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @GetMapping
    public ApiResponse<CursorPageResponse<FeedResponse>> getFeedList(
            @CurrentUser User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) FeedStatus feedStatus,
            @RequestParam(name = "category", required = false) List<FeedCategory> categories
    ) {
        CursorPageResponse<FeedResponse> response = feedFacade.getFeedList(user, cursor, size, feedStatus, categories);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @GetMapping("/hot")
    public ApiResponse<StringCursorPageResponse<FeedResponse>> getHotFeedList(
            @CurrentUser User user,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "category", required = false) List<FeedCategory> categories
    ) {
        StringCursorPageResponse<FeedResponse> response = feedFacade.getHotFeedList(user, cursor, size, categories);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @GetMapping("/{feedId}")
    public ApiResponse<FeedResponse> getFeedDetail(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        FeedResponse response = feedFacade.getFeedDetail(user, feedId);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{feedId}")
    public ApiResponse<Void> deleteFeed(@CurrentUser User user, @PathVariable Long feedId) {
        feedFacade.deleteFeed(user, feedId);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{feedId}/guest")
    public ApiResponse<Void> deleteGuestFeed(
            @PathVariable Long feedId,
            @Valid @RequestBody FeedGuestDeleteRequest request
    ) {
        feedFacade.deleteGuestFeed(feedId, request);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @PostMapping("/{feedId}/report")
    public ApiResponse<Void> reportFeed(@CurrentUser User user, @PathVariable Long feedId) {
        feedFacade.reportFeed(user, feedId);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @PostMapping("/ai-draft")
    public ApiResponse<FeedAiDraftResponse> createAiDraft(
            @CurrentUser User user,
            @Valid @RequestBody FeedAiDraftRequest request
    ) {
        FeedAiDraftResponse response = feedAiDraftFacade.createDraft(user, request);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @PostMapping("/ai-draft/guest")
    public ApiResponse<FeedAiDraftResponse> createGuestAiDraft(
            @Valid @RequestBody FeedAiDraftRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest
    ) {
        String ip = ClientIpResolver.resolve(httpRequest);
        FeedAiDraftResponse response = feedAiDraftFacade.createGuestDraft(request, ip, deviceId);
        return ApiResponse.success(response, HttpStatus.OK);
    }
}
