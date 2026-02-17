package com.nexters.sseotdabwa.api.users.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.api.users.facade.UserFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ApiResponse<List<FeedResponse>> getMyFeeds(@CurrentUser User user) {
        List<FeedResponse> response = userFacade.getMyFeeds(user);
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
}
