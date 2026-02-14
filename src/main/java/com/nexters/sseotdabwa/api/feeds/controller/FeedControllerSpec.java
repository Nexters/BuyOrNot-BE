package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.common.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Feeds", description = "피드 API")
public interface FeedControllerSpec {

    @Operation(
            summary = "피드(투표) 등록",
            description = """
                    피드(투표) 등록 API입니다.

                    - 필수 입력: category, price, s3ObjectKey, imageWidth, imageHeight
                    - 이미지는 Presigned URL 업로드 방식이며, 현재 API는 업로드 완료된 s3ObjectKey만 받습니다.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "피드 등록 성공",
                    content = @Content(schema = @Schema(implementation = FeedCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (필수값 누락/정책 위반)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    ApiResponse<FeedCreateResponse> createFeed(
            @Parameter(hidden = true) User user,
            @Valid @RequestBody FeedCreateRequest request
    );

    @Operation(
            summary = "피드 리스트 조회",
            description = "전체 피드 리스트를 조회합니다. 비로그인 유저도 접근 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "피드 리스트 조회 성공"
            )
    })
    ApiResponse<List<FeedResponse>> getFeedList();

    @Operation(
            summary = "피드 삭제",
            description = "본인이 작성한 피드를 삭제합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "피드 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 피드만 삭제 가능"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<Void> deleteFeed(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId
    );
}
