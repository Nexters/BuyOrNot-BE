package com.nexters.sseotdabwa.api.auth.controller;

import com.nexters.sseotdabwa.api.auth.dto.KakaoLoginRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenRefreshRequest;
import com.nexters.sseotdabwa.api.auth.dto.TokenResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

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
