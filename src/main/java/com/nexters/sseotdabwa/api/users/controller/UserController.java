package com.nexters.sseotdabwa.api.users.controller;

import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.api.users.facade.UserFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
}
