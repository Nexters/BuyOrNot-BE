package com.nexters.sseotdabwa.api.comments.controller;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.users.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Comments",
        description = "피드 댓글(의견) API. 투표 여부와 무관하게 회원/게스트 모두 작성 가능하며, "
                + "투표 여부는 댓글 작성 자격이 아니라 응답의 isAuthor/voteChoice(태그·버블 노출용)에만 영향을 준다."
)
public interface CommentControllerSpec {

    @Operation(
            summary = "회원 댓글 작성",
            description = """
                    회원이 피드에 댓글(의견)을 남긴다.

                    - 투표 여부와 무관하게 작성 가능 (투표는 자격 조건이 아니라 응답 태그/버블 표시에만 영향)
                    - 닉네임은 항상 본인의 회원 닉네임(user.nickname)을 그대로 사용, 별도 입력 불필요
                    - 내용은 공백 제거 후 1~300자여야 함
                    - 마감(작성 후 48시간 경과)되었거나 신고 삭제된 피드에는 작성 불가
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용 누락(COMMENT_001) / 300자 초과(COMMENT_002) / 마감된 피드(COMMENT_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음(FEED_003)")
    })
    ApiResponse<CommentCreateResponse> createComment(
            @Parameter(hidden = true) User user,
            @Parameter(description = "댓글을 작성할 피드 id", required = true) @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequest request
    );

    @Operation(
            summary = "게스트 댓글 작성",
            description = """
                    게스트(비회원)가 피드에 댓글(의견)을 남긴다. 인증 불필요.

                    - 투표 여부와 무관하게 작성 가능
                    - guestNickname은 `GET /api/v1/guest/nickname`으로 발급받은 값을 그대로 전달. \
                    실제 형용사+명사 조합이 아니면(클라이언트 조작 등) 서버가 새로 발급해서 대체하며, 같은 피드 안에서 \
                    닉네임이 이미 쓰이고 있으면 자동으로 재발급한다(최대 5회 재시도)
                    - guestPassword는 이후 본인 확인(삭제 시)을 위해 BCrypt 해시로만 저장되고 평문으로는 저장되지 않음. \
                    삭제할 때 이 비밀번호를 다시 입력해야 하므로 클라이언트가 반드시 기억해두어야 함(서버는 복구 수단 제공 안 함)
                    - 내용은 공백 제거 후 1~300자여야 함
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용/닉네임/비밀번호 누락(COMMENT_001) / 300자 초과(COMMENT_002) / 마감된 피드(COMMENT_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음(FEED_003)")
    })
    ApiResponse<CommentCreateResponse> createGuestComment(
            @Parameter(description = "댓글을 작성할 피드 id", required = true) @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequestGuest request
    );

    @Operation(
            summary = "댓글 목록 조회",
            description = """
                    피드의 댓글(의견) 목록을 커서 기반 페이지네이션으로 조회한다. 인증 불필요(로그인 상태면 isMine 계산에 사용).

                    - 신고 처리된 댓글은 목록/카운트에서 제외됨
                    - 각 댓글의 isAuthor는 그 댓글 작성자가 이 피드의 원작성자 본인인지("작성자" 태그 표시용, 피드 작성자는 \
                    자기 피드에 투표할 수 없어 voteChoice는 항상 null)
                    - voteChoice는 댓글 작성자(회원만 해당)가 이 피드에 투표했다면 그 선택(YES/NO). 게스트 댓글이거나 \
                    투표하지 않은 회원의 댓글이면 항상 null → 프론트에서 태그/버블 미노출 처리
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음(FEED_003)")
    })
    ApiResponse<CursorPageResponse<CommentResponse>> getComments(
            @Parameter(hidden = true) User user,
            @Parameter(description = "댓글을 조회할 피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "커서(마지막으로 받은 댓글 id). 첫 페이지는 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (기본 20, 최대 50)") @RequestParam(required = false) Integer size,
            @Parameter(description = "정렬 기준. REGISTERED(등록순, 기본값) 또는 LATEST(최신순)") @RequestParam(required = false) CommentSort sort
    );

    @Operation(
            summary = "회원 댓글 삭제",
            description = "본인이 작성한 댓글만 삭제할 수 있다(비밀번호 없이 소유자 검증만). 삭제는 물리 삭제이며 복구할 수 없다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 댓글이 아님(COMMENT_005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음(COMMENT_004)")
    })
    ApiResponse<Void> deleteComment(
            @Parameter(hidden = true) User user,
            @Parameter(description = "댓글이 속한 피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "삭제할 댓글 id", required = true) @PathVariable Long commentId
    );

    @Operation(
            summary = "게스트 댓글 삭제",
            description = """
                    게스트가 작성한 댓글을 삭제한다. 인증 불필요.

                    게스트는 서버가 개별 신원을 식별할 방법이 없으므로(세션/디바이스ID 없음), 클라이언트는 어떤 게스트 댓글에도 \
                    삭제를 시도할 수 있고 실질적인 권한 검사는 작성 시 입력한 비밀번호 일치 여부로만 이루어진다. \
                    회원이 작성한 댓글에는 이 API로 삭제를 시도할 수 없다(COMMENT_006). 삭제는 물리 삭제이며 복구할 수 없다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "비회원 댓글이 아님(COMMENT_006) 또는 비밀번호 불일치(COMMENT_007)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음(COMMENT_004)")
    })
    ApiResponse<Void> deleteGuestComment(
            @Parameter(description = "댓글이 속한 피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "삭제할 댓글 id", required = true) @PathVariable Long commentId,
            @Valid @RequestBody CommentGuestDeleteRequest request
    );

    @Operation(
            summary = "댓글 신고",
            description = """
                    댓글을 신고한다. 인증 불필요 — 회원/게스트 누구나(로그인 여부 무관) 신고할 수 있다.

                    - 신고 누적 임계치 없음. **1건만 접수되어도 즉시 숨김 처리**되어 이후 목록/카운트/프리뷰에서 제외됨(Feed 신고와 동일 정책)
                    - 로그인한 회원이 본인이 작성한 댓글을 신고하려 하면 거부됨(자기신고 방지). 게스트는 신원 식별이 불가능해 \
                    이 검사 대상이 아님(게스트 자신의 댓글도 신고 자체는 가능)
                    - 이미 신고 처리된 댓글은 재신고 불가
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 접수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본인 댓글 자기신고(COMMENT_008) / 이미 신고된 댓글(COMMENT_009)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음(COMMENT_004)")
    })
    ApiResponse<Void> reportComment(
            @Parameter(hidden = true) User user,
            @Parameter(description = "댓글이 속한 피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "신고할 댓글 id", required = true) @PathVariable Long commentId
    );
}
