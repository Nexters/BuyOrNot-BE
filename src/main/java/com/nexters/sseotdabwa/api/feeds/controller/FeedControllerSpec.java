package com.nexters.sseotdabwa.api.feeds.controller;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Feeds", description = "피드 API")
public interface FeedControllerSpec {

    @Operation(
            summary = "피드(투표) 등록",
            description = """
                    피드(투표) 등록 API입니다.

                    - 필수 입력: category, price, content(<=100자), s3ObjectKey, imageWidth, imageHeight
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
}
