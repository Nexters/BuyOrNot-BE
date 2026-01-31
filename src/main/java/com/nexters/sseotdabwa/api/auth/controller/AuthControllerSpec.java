package com.nexters.sseotdabwa.api.auth.controller;

import com.nexters.sseotdabwa.api.auth.dto.GoogleLoginRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;

import com.nexters.sseotdabwa.api.auth.dto.AppleLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerSpec {

    @Operation(
            summary = "[카카오] 소셜 로그인",
            description = """
                    카카오 소셜 로그인 API입니다.

                    클라이언트(Android/iOS/Web)에서 카카오 SDK로 발급받은 Access Token을 전달하면,
                    서버에서 카카오 API를 통해 사용자 정보를 조회하고 JWT 토큰을 발급합니다.

                    - 신규 사용자: 자동 회원가입 후 토큰 발급
                    - 기존 사용자: 프로필 정보 업데이트 후 토큰 발급
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 (JWT 토큰 + 사용자 정보 반환)",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 카카오 액세스 토큰"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "카카오 API 호출 실패"
            )
    })
    ApiResponse<TokenResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request);

    @Operation(
            summary = "[Apple] 소셜 로그인",
            description = """
                    Apple 소셜 로그인 API입니다.

                    클라이언트(iOS/Web)에서 Apple SDK로 발급받은 Authorization Code를 전달하면,
                    서버에서 Apple Token API를 호출하여 사용자를 검증하고 JWT 토큰을 발급합니다.

                    - 신규 사용자: 자동 회원가입 후 토큰 발급 (랜덤 닉네임/프로필 이미지 부여)
                    - 기존 사용자: 토큰 발급

                    **redirectUri 파라미터:**
                    - iOS 앱 (ASAuthorizationAppleIDProvider): 생략 가능
                    - Web: 필수 (초기 인증 요청에서 사용한 redirect_uri와 동일한 값)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 (JWT 토큰 + 사용자 정보 반환)",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 Apple Identity Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Apple 공개키 조회 실패"
            )
    })
    ApiResponse<TokenResponse> loginWithApple(@Valid @RequestBody AppleLoginRequest request);

    @Operation(
            summary = "[Google] 소셜 로그인",
            description = """
                    Google 소셜 로그인 API입니다.

                    클라이언트(iOS/Android/Web)에서 Google SDK로 발급받은 ID Token을 전달하면,
                    서버에서 Google 공개키로 토큰을 검증하고 JWT 토큰을 발급합니다.

                    - 신규 사용자: 자동 회원가입 후 토큰 발급 (랜덤 닉네임/프로필 이미지 부여)
                    - 기존 사용자: 토큰 발급
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 (JWT 토큰 + 사용자 정보 반환)",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 Google ID Token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Google 공개키 조회 실패"
            )
    })
    ApiResponse<TokenResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request);

    @Operation(
            summary = "토큰 갱신",
            description = """
                    JWT Access Token 갱신 API입니다.

                    Refresh Token으로 새로운 Access Token을 발급받습니다.
                    Access Token 만료 시(1시간) 이 API를 호출하세요.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰"
            )
    })
    ApiResponse<TokenResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request);
}
