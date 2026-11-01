package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequestV2;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponseV2;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.response.StringCursorPageResponse;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Feeds V2", description = "피드 API V2 (다중 이미지 지원)")
public interface FeedControllerSpecV2 {

    @Operation(
            summary = "피드(투표) 등록 V2",
            description = """
                    피드(투표) 등록 API V2입니다.

                    - 필수 입력: category, price, s3ObjectKeys (1~3장), imageWidth, imageHeight
                    - 이미지는 Presigned URL 업로드 방식이며, 업로드 완료된 s3ObjectKey 목록을 받습니다.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "피드 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (필수값 누락/정책 위반)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<FeedCreateResponse> createFeed(
            @Parameter(hidden = true) User user,
            @Valid @RequestBody FeedCreateRequestV2 request
    );

    @Operation(
            summary = "피드 리스트 조회 V2",
            description = "커서 기반 페이지네이션으로 피드 리스트를 조회합니다. 다중 이미지(imageUrls)를 반환합니다. 비로그인 유저도 접근 가능하며, 로그인 시 투표 상태가 포함됩니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드 리스트 조회 성공")
    })
    ApiResponse<CursorPageResponse<FeedResponseV2>> getFeedList(
            @Parameter(hidden = true) User user,
            @Parameter(description = "이전 페이지 마지막 feedId (첫 페이지는 생략)") Long cursor,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 50)") Integer size,
            @Parameter(description = "피드 상태 필터 (OPEN, CLOSED / 미지정 시 전체)") FeedStatus feedStatus,
            @Parameter(name = "category", description = "카테고리 필터 - 복수 선택 가능 (?category=BOOK&category=FASHION / 미지정 시 전체)") List<FeedCategory> categories
    );

    @Operation(
            summary = "피드 단건 조회 V2",
            description = "피드 ID로 단건 조회합니다. 다중 이미지(imageUrls)를 반환합니다. 비로그인 유저도 접근 가능하며, 로그인 시 투표 상태가 포함됩니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드 단건 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    ApiResponse<FeedResponseV2> getFeedDetail(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId
    );

    @Operation(
            summary = "핫플 투표 리스트 조회 V2",
            description = """
                    진행 중인 투표를 투표 수가 많은 순으로 조회합니다. (핫플 투표🔥 탭)

                    - 대상: 진행중(OPEN)이면서 마감 시각(작성 후 48시간)이 지나지 않은 피드. 신고/삭제 피드 제외, 로그인 시 차단 유저 피드 제외
                    - 정렬: 투표 수(yesCount + noCount) 내림차순, 동률이면 최신순(feedId 내림차순). 투표 0개 피드도 맨 뒤에 최신순으로 포함
                    - 커서: 응답의 nextCursor 문자열("{투표수}_{feedId}")을 그대로 다음 요청의 cursor로 전달 (첫 페이지는 생략)
                    - 투표 수가 실시간으로 바뀌어 페이지 사이에 같은 피드가 다시 나올 수 있으므로 클라이언트에서 feedId 기준 중복 제거를 권장합니다.
                    - 비로그인 유저도 접근 가능하며, 로그인 시 투표 상태가 포함됩니다.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "핫플 투표 리스트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 커서 (FEED_013)")
    })
    ApiResponse<StringCursorPageResponse<FeedResponseV2>> getHotFeedList(
            @Parameter(hidden = true) User user,
            @Parameter(description = "이전 응답의 nextCursor (\"{투표수}_{feedId}\", 첫 페이지는 생략)") String cursor,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 50)") Integer size,
            @Parameter(name = "category", description = "카테고리 필터 - 복수 선택 가능 (?category=BOOK&category=FASHION / 미지정 시 전체)") List<FeedCategory> categories
    );
}
