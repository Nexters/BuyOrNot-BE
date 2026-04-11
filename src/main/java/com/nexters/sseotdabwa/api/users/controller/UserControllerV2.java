package com.nexters.sseotdabwa.api.users.controller;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponseV2;
import com.nexters.sseotdabwa.api.users.facade.UserFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
public class UserControllerV2 implements UserControllerSpecV2 {

    private final UserFacade userFacade;

    @Override
    @GetMapping("/me/feeds")
    public ApiResponse<CursorPageResponse<FeedResponseV2>> getMyFeeds(
            @CurrentUser User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) FeedStatus feedStatus
    ) {
        CursorPageResponse<FeedResponseV2> response = userFacade.getMyFeedsV2(user, cursor, size, feedStatus);
        return ApiResponse.success(response, HttpStatus.OK);
    }
}
