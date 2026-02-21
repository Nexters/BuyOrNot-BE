package com.nexters.sseotdabwa.api.users.controller;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Users", description = "사용자 API")
public interface UserControllerSpec {

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    ApiResponse<UserResponse> getMyInfo(@Parameter(hidden = true) User user);

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 삭제합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    ApiResponse<UserWithdrawResponse> withdraw(@Parameter(hidden = true) User user);

    @Operation(
            summary = "내가 작성한 피드 조회",
            description = "커서 기반 페이지네이션으로 현재 로그인한 사용자가 작성한 피드 목록을 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    ApiResponse<CursorPageResponse<FeedResponse>> getMyFeeds(
            @Parameter(hidden = true) User user,
            @Parameter(description = "이전 페이지 마지막 feedId (첫 페이지는 생략)") Long cursor,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 50)") Integer size,
            @Parameter(description = "피드 상태 필터 (OPEN, CLOSED / 미지정 시 전체)") FeedStatus feedStatus
    );

    @Operation(
            summary = "FCM 토큰 등록/갱신",
            description = "현재 로그인한 사용자의 FCM 토큰을 저장(업데이트)합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> updateFcmToken(
            @Parameter(hidden = true) User user,
            @RequestBody FcmTokenRequest request
    );
}
