package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedAiDraftRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedAiDraftResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequestGuest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateResponse;
import com.nexters.sseotdabwa.api.feeds.dto.FeedGuestDeleteRequest;
import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
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
import jakarta.servlet.http.HttpServletRequest;
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
                    description = "피드 등록 성공"
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
            summary = "게스트(비회원) 피드 등록",
            description = """
                    비회원이 피드(투표)를 등록합니다.

                    - 인증 불필요
                    - guestNickname: `/api/v1/guest/nickname`으로 발급받은 닉네임. 실제 발급된 조합이 아니면 서버가 새로 발급해서 대체합니다.
                    - guestPassword: 이후 삭제 시 본인 확인에 사용되는 비밀번호 (해시로 저장)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "게스트 피드 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (필수값 누락/정책 위반)"
            )
    })
    ApiResponse<FeedCreateResponse> createGuestFeed(
            @Valid @RequestBody FeedCreateRequestGuest request
    );

    @Operation(
            summary = "피드 리스트 조회",
            description = "커서 기반 페이지네이션으로 피드 리스트를 조회합니다. 비로그인 유저도 접근 가능하며, 로그인 시 투표 상태가 포함됩니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "피드 리스트 조회 성공"
            )
    })
    ApiResponse<CursorPageResponse<FeedResponse>> getFeedList(
            @Parameter(hidden = true) User user,
            @Parameter(description = "이전 페이지 마지막 feedId (첫 페이지는 생략)") Long cursor,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 50)") Integer size,
            @Parameter(description = "피드 상태 필터 (OPEN, CLOSED / 미지정 시 전체)") FeedStatus feedStatus,
            @Parameter(name = "category", description = "카테고리 필터 - 복수 선택 가능 (?category=BOOK&category=FASHION / 미지정 시 전체)") List<FeedCategory> categories
    );

    @Operation(
            summary = "피드 단건 조회",
            description = "피드 ID로 단건 조회합니다. 비로그인 유저도 접근 가능하며, 로그인 시 투표 상태가 포함됩니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "피드 단건 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<FeedResponse> getFeedDetail(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId
    );

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

    @Operation(
            summary = "게스트(비회원) 피드 삭제",
            description = "비회원이 작성한 피드를 등록 시 설정한 비밀번호로 삭제합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게스트 피드 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "비밀번호 불일치 또는 회원이 작성한 피드"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<Void> deleteGuestFeed(
            @PathVariable Long feedId,
            @Valid @RequestBody FeedGuestDeleteRequest request
    );

    @Operation(
            summary = "피드 신고",
            description = "피드를 신고합니다. 본인의 피드는 신고할 수 없습니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "피드 신고 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "본인 피드 신고 불가 또는 이미 신고된 피드"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<Void> reportFeed(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId
    );

    @Operation(
            summary = "핫플 투표 리스트 조회",
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
    ApiResponse<StringCursorPageResponse<FeedResponse>> getHotFeedList(
            @Parameter(hidden = true) User user,
            @Parameter(description = "이전 응답의 nextCursor (\"{투표수}_{feedId}\", 첫 페이지는 생략)") String cursor,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 50)") Integer size,
            @Parameter(name = "category", description = "카테고리 필터 - 복수 선택 가능 (?category=BOOK&category=FASHION / 미지정 시 전체)") List<FeedCategory> categories
    );

    @Operation(
            summary = "사진으로 AI 피드 초안 생성",
            description = """
                    업로드한 상품 사진으로 AI가 피드(투표) 글의 제목/내용/카테고리 초안을 만들어줍니다. (회원용, 앱/웹 공통 — 비회원은 /ai-draft/guest)

                    - 초안은 저장되지 않습니다. 폼에 채운 뒤 사용자가 수정해서 기존 피드 등록 API로 등록하세요.
                    - 요청: 기존 presigned 업로드(POST /api/v1/uploads/presigned-put)로 올린 s3ObjectKey 1~3장
                    - 응답: title(40자 이하), content(100자 이하), category(애매하면 ETC), price
                      - 금칙어가 포함되거나 비어 있으면 title/content는 null
                      - price: 사진에 원화 가격이 글자로 보일 때만 금액(원, 할인가 우선), 아니면 null (추정가는 넣지 않음)
                      - price는 필수 입력값이므로 null이면 사용자가 직접 입력해야 하고, 값이 있어도 잘못 읽었을 수 있어 확인 안내 권장
                    - HEIC 사진은 AI 초안 대상에서 제외됩니다 (모두 HEIC면 FEED_015)
                    - 응답까지 수 초 걸릴 수 있어 로딩 UI가 필요합니다.
                    - 빈도 제한: 계정당 분당 5회, 하루 20회
                    - 실패(FEED_017, FEED_018, 429)해도 피드 등록은 가능해야 하므로 직접 입력 화면을 유지하세요.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초안 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / feeds 경로가 아닌 key (FEED_014) / 지원하지 않는 이미지 포맷 (FEED_015)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 빈도 제한 초과 (FEED_016)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 초안 생성 실패 (FEED_017) / 기능 비활성 (FEED_018)")
    })
    ApiResponse<FeedAiDraftResponse> createAiDraft(
            @Parameter(hidden = true) User user,
            @Valid @RequestBody FeedAiDraftRequest request
    );

    @Operation(
            summary = "사진으로 AI 피드 초안 생성 (비회원)",
            description = """
                    비회원용 AI 초안 생성 API입니다. 인증 없이 호출하며, 요청/응답/에러는 회원용(POST /api/v1/feeds/ai-draft)과 같습니다.

                    - 초안은 저장되지 않습니다. 폼에 채운 뒤 기존 게스트 피드 등록 API(POST /api/v1/feeds/guest)로 등록하세요.
                    - 빈도 제한: IP와 디바이스(`X-Device-Id`) 각각 분당 5회, 하루 20회 (어느 하나라도 넘으면 FEED_016)
                    - `X-Device-Id`는 선택이지만, 보내지 않으면 IP 기준으로만 제한되어 같은 네트워크의 다른 사용자와 횟수를 나눠 쓸 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초안 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / feeds 경로가 아닌 key (FEED_014) / 지원하지 않는 이미지 포맷 (FEED_015)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 빈도 제한 초과 (FEED_016)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 초안 생성 실패 (FEED_017) / 기능 비활성 (FEED_018)")
    })
    ApiResponse<FeedAiDraftResponse> createGuestAiDraft(
            @Valid @RequestBody FeedAiDraftRequest request,
            @Parameter(description = "클라이언트 디바이스 식별자(요청 빈도 제한용, 선택)") String deviceId,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    );
}
