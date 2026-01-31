package com.nexters.sseotdabwa.api.auth.controller;

import com.nexters.sseotdabwa.api.auth.dto.GoogleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.AppleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenResponse;
import com.nexters.sseotdabwa.api.auth.facade.AuthFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러
 * - 카카오 소셜 로그인
 * - Apple 소셜 로그인
 * - Google 소셜 로그인
 * - Access Token 갱신
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSpec {

    private final AuthFacade authFacade;

    @Override
    @PostMapping("/kakao/login")
    public ApiResponse<TokenResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        TokenResponse response = authFacade.loginWithKakao(request);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @PostMapping("/apple/login")
    public ApiResponse<TokenResponse> loginWithApple(@Valid @RequestBody AppleLoginRequest request) {
        TokenResponse response = authFacade.loginWithApple(request);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @PostMapping("/google/login")
    public ApiResponse<TokenResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        TokenResponse response = authFacade.loginWithGoogle(request);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenResponse response = authFacade.refreshToken(request);
        return ApiResponse.success(response, HttpStatus.OK);
    }
}
