package com.nexters.sseotdabwa.api.users.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponseV2;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users V2", description = "사용자 API V2")
public interface UserControllerSpecV2 {

    @Operation(
            summary = "내가 작성한 피드 조회 V2",
            description = "커서 기반 페이지네이션으로 현재 로그인한 사용자가 작성한 피드 목록을 조회합니다. 다중 이미지(imageUrls)를 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<CursorPageResponse<FeedResponseV2>> getMyFeeds(
            @Parameter(hidden = true) User user,
            @Parameter(description = "이전 페이지 마지막 feedId (첫 페이지는 생략)") Long cursor,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 50)") Integer size,
            @Parameter(description = "피드 상태 필터 (OPEN, CLOSED / 미지정 시 전체)") FeedStatus feedStatus,
            @Parameter(name = "category", description = "카테고리 필터 - 복수 선택 가능 (?category=BOOK&category=FASHION / 미지정 시 전체)") List<FeedCategory> categories
    );
}
