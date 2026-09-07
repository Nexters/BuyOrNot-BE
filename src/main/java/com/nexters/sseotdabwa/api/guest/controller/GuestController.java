package com.nexters.sseotdabwa.api.guest.controller;

import com.nexters.sseotdabwa.api.guest.dto.GuestNicknameResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.domain.users.service.RandomNicknameGenerator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest")
@RequiredArgsConstructor
public class GuestController implements GuestControllerSpec {

    private final RandomNicknameGenerator randomNicknameGenerator;

    @Override
    @GetMapping("/nickname")
    public ApiResponse<GuestNicknameResponse> issueNickname() {
        String nickname = randomNicknameGenerator.generate();
        return ApiResponse.success(GuestNicknameResponse.of(nickname), HttpStatus.OK);
    }
}
