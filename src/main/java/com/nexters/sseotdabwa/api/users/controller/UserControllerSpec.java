package com.nexters.sseotdabwa.api.users.controller;

import java.util.List;

import com.nexters.sseotdabwa.api.feeds.dto.FeedResponse;
import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;
import com.nexters.sseotdabwa.api.users.dto.UserProfileUpdateRequest;
import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.api.users.dto.UserWithdrawResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.api.users.dto.BlockedUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestBody;

@Tag(
        name = "Users",
        description = "사용자 API. 참고: 닉네임이 아직 없는(가입 직후) 회원은 이 태그의 프로필 수정/내 정보 조회와 로그아웃을 "
                + "제외한 모든 인증 API에서 403(USER_006)을 받는다 — 먼저 프로필 수정 API로 닉네임을 설정해야 한다."
)
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
            summary = "프로필 수정 (닉네임 최초 설정 겸용)",
            description = """
                    닉네임/프로필 이미지를 수정한다. 둘 다 optional — 요청에 포함하지 않으면(또는 null) 해당 필드는 변경하지 않는다.

                    - 회원가입 직후에는 닉네임이 비어있는 상태(null)라, 다른 API는 이 API(및 내 정보 조회, 로그아웃)를 \
                    제외하고 전부 403(USER_006)으로 막힌다. 이 API로 닉네임을 최초 설정해야 서비스 진입이 가능하다.
                    - 닉네임 규칙: 완성형 한글/영문/숫자만 3~10자, 자음·모음 단독이나 숫자로만 구성 불가, 특수문자·이모지·중간 \
                    공백 불가(앞뒤 공백은 자동 제거), 대소문자 무관 중복 불가, 사칭 키워드(운영자/관리자/공식 등) 불가
                    - 닉네임을 이미 가진 상태에서 변경하는 경우, 마지막 변경일로부터 20일이 지나야 함(최초 설정은 예외)
                    - 요청한 닉네임이 현재 값과 같으면(대소문자 무관) 아무 것도 하지 않고 그대로 성공 처리
                    - 프로필 이미지는 변경 주기 제한 없음
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "닉네임 형식 오류(USER_007~010) / 사칭 키워드(USER_012) / 변경 주기 제한(USER_013)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임(USER_011)")
    })
    ApiResponse<UserResponse> updateProfile(
            @Parameter(hidden = true) User user,
            @RequestBody UserProfileUpdateRequest request
    );

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
            @Parameter(description = "피드 상태 필터 (OPEN, CLOSED / 미지정 시 전체)") FeedStatus feedStatus,
            @Parameter(name = "category", description = "카테고리 필터 - 복수 선택 가능 (?category=BOOK&category=FASHION / 미지정 시 전체)") List<FeedCategory> categories
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

    @Operation(
            summary = "사용자 차단",
            description = "특정 사용자를 차단합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "차단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 차단 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 차단됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> blockUser(
            @Parameter(hidden = true) User user,
            @Parameter(description = "차단할 사용자 ID") Long userId
    );

    @Operation(
            summary = "차단 사용자 목록 조회",
            description = "현재 로그인한 사용자가 차단한 사용자 목록을 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<List<BlockedUserResponse>> getBlockedUsers(@Parameter(hidden = true) User user);

    @Operation(
            summary = "사용자 차단 해제",
            description = "특정 사용자 차단을 해제합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차단 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "차단 관계 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> unblockUser(
            @Parameter(hidden = true) User user,
            @Parameter(description = "차단 해제할 사용자 ID") Long userId
    );

    @Operation(
            summary = "앱 오픈 기록",
            description = "앱 실행 시 호출하여 마지막 오픈 시각을 기록합니다. 마케팅 푸시 발송 대상 선정에 사용됩니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<Void> recordAppOpen(@Parameter(hidden = true) User user);
}
