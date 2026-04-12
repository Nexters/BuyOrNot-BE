package com.nexters.sseotdabwa.api.users.controller;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.users.dto.BlockedUserResponse;
import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.api.users.facade.UserFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

    private final UserFacade userFacade;

    @Override
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(@CurrentUser User user) {
        UserResponse response = userFacade.getMyInfo(user);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/me")
    public ApiResponse<UserWithdrawResponse> withdraw(@CurrentUser User user) {
        UserWithdrawResponse response = userFacade.withdraw(user);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @GetMapping("/me/feeds")
    public ApiResponse<CursorPageResponse<FeedResponse>> getMyFeeds(
            @CurrentUser User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) FeedStatus feedStatus,
            @RequestParam(required = false) FeedCategory category
    ) {
        CursorPageResponse<FeedResponse> response = userFacade.getMyFeeds(user, cursor, size, feedStatus, category);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @PatchMapping("/fcm")
    public ApiResponse<Void> updateFcmToken(
            @CurrentUser User user,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        userFacade.updateFcmToken(user, request);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @PostMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> blockUser(
            @CurrentUser User user,
            @PathVariable Long userId
    ) {
        userFacade.blockUser(user, userId);
        return ApiResponse.success(HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/blocks")
    public ApiResponse<List<BlockedUserResponse>> getBlockedUsers(
            @CurrentUser User user
    ) {
        List<BlockedUserResponse> response = userFacade.getBlockedUsers(user);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/blocks/{userId}")
    public ApiResponse<Void> unblockUser(
            @CurrentUser User user,
            @PathVariable Long userId
    ) {
        userFacade.unblockUser(user, userId);
        return ApiResponse.success(HttpStatus.OK);
    }
}
