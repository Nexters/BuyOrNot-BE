package com.nexters.sseotdabwa.api.comments.controller;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentLikeResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentReplyResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentThreadResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Comments",
        description = "피드 댓글(의견)/답글/공감 API. 원댓글은 투표 여부와 무관하게 회원/게스트 모두 작성 가능하며, "
                + "투표 여부는 댓글 작성 자격이 아니라 응답의 isAuthor/voteChoice(태그·버블 노출용)에만 영향을 준다. "
                + "답글(원댓글에만, depth 1)은 회원/게스트 모두, 공감(원댓글에만)은 회원만 가능하다. 마감된 피드에도 모두 가능하다."
)
public interface CommentControllerSpec {

    @Operation(
            summary = "회원 댓글 작성",
            description = """
                    회원이 피드에 댓글(의견)을 남긴다.

                    - 투표 여부와 무관하게 작성 가능 (투표는 자격 조건이 아니라 응답 태그/버블 표시에만 영향)
                    - 닉네임은 항상 본인의 회원 닉네임(user.nickname)을 그대로 사용, 별도 입력 불필요
                    - 내용은 공백 제거 후 1~300자여야 함
                    - 마감된 피드에도 작성 가능
                    - 계정(userId) 기준 분당 5회로 요청 빈도가 제한됨(콘텐츠 내용과 무관). 웹 요청은 프록시를 거쳐 \
                    IP가 공유되므로 IP가 아닌 계정 기준으로 제한함
                    - 금칙어(욕설) 포함 시 400 반환. 같은 작성자가 10분 내 5회 이상 금칙어 위반 시, 이후 요청은 \
                    내용과 무관하게 5분간 즉시 차단됨(요청 빈도 제한과는 별개의 정책)
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용 누락(COMMENT_001) / 300자 초과(COMMENT_002) / 금칙어 포함(COMMENT_011)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음(FEED_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 빈도 초과(COMMENT_010) / 금칙어 반복 위반으로 일시 차단(COMMENT_012)")
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
                    - IP/디바이스(`X-Device-Id`) 기준 분당 5회로 요청 빈도가 제한됨(콘텐츠 내용과 무관)
                    - 금칙어(욕설) 포함 시 400 반환. 같은 게스트(IP+디바이스ID 기준)가 10분 내 5회 이상 금칙어 위반 시, \
                    이후 요청은 내용과 무관하게 5분간 즉시 차단됨(요청 빈도 제한과는 별개의 정책)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용/닉네임/비밀번호 누락(COMMENT_001) / 300자 초과(COMMENT_002) / 금칙어 포함(COMMENT_011)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음(FEED_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 빈도 초과(COMMENT_010) / 금칙어 반복 위반으로 일시 차단(COMMENT_012)")
    })
    ApiResponse<CommentCreateResponse> createGuestComment(
            @Parameter(description = "댓글을 작성할 피드 id", required = true) @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequestGuest request,
            @Parameter(description = "클라이언트 디바이스 식별자(요청 빈도 제한용, 선택)") @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(
            summary = "댓글 목록 조회",
            description = """
                    피드의 원댓글 목록을 커서 기반 페이지네이션으로 조회한다. 인증 불필요(로그인 상태면 isMine/isLiked 계산에 사용).

                    - 페이지네이션/정렬은 원댓글 기준. 각 원댓글의 replies에 답글 전체가 등록순으로 붙는다
                    - 신고 처리된 댓글/답글은 제외됨. 신고된 원댓글은 답글까지 함께 제외됨
                    - likeCount/isLiked/replyCount로 "공감"/"답글쓰기" 텍스트 대신 숫자 노출 여부를 판단
                    - 답글에는 투표 태그/버블(voteChoice)이 없음 — 투표 내용은 원댓글에만 노출
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
            description = """
                    본인이 작성한 댓글/답글만 삭제할 수 있다(비밀번호 없이 소유자 검증만). 복구할 수 없다.

                    - 원댓글을 삭제하면 그 원댓글의 답글과 공감까지 모두 삭제된다("삭제된 댓글"로 남기지 않음)
                    - 답글을 삭제하면 해당 답글만 삭제된다
                    """,
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
                    회원이 작성한 댓글에는 이 API로 삭제를 시도할 수 없다(COMMENT_006). 복구할 수 없으며, \
                    원댓글을 지우면 답글까지 함께 삭제된다(회원 댓글 삭제와 같은 정책).
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

    @Operation(
            summary = "회원 답글 작성",
            description = """
                    원댓글에 답글을 남긴다. 회원만 가능.

                    - 원댓글에만 작성 가능(답글에 답글 불가, depth 1)
                    - 신고된 원댓글에는 작성 불가
                    - 마감된 피드에도 작성 가능
                    - 내용 규칙, 요청 빈도 제한(계정 기준 분당 5회), 금칙어 정책은 원댓글 작성과 동일하며 같은 카운터를 공유
                    - 작성 후 원댓글 작성자 + 이 스레드에 답글을 단 회원들에게 알림이 발송됨(본인, 답글 작성자를 차단한 회원 제외). \
                    앱 회원은 푸시, 푸시를 받을 수 없는 웹 회원은 이메일(같은 스레드는 30분에 1통)
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "답글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용 누락(COMMENT_001) / 300자 초과(COMMENT_002) / 금칙어 포함(COMMENT_011) / 답글에 답글(COMMENT_013) / 신고된 원댓글(COMMENT_014)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드(FEED_003) 또는 원댓글(COMMENT_004)을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 빈도 초과(COMMENT_010) / 금칙어 반복 위반으로 일시 차단(COMMENT_012)")
    })
    ApiResponse<CommentCreateResponse> createReply(
            @Parameter(hidden = true) User user,
            @Parameter(description = "피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "답글을 달 원댓글 id", required = true) @PathVariable Long commentId,
            @Valid @RequestBody CommentCreateRequest request
    );

    @Operation(
            summary = "게스트 답글 작성",
            description = """
                    게스트(비회원)가 원댓글에 답글을 남긴다. 인증 불필요.

                    - 요청 형식/검증은 게스트 댓글 작성과 동일 — guestNickname은 `GET /api/v1/guest/nickname`으로 발급받은 값, \
                    guestPassword는 이후 삭제할 때 다시 입력해야 하므로 클라이언트가 비밀번호 입력 다이얼로그로 받아서 전달
                    - IP/디바이스(`X-Device-Id`) 기준 분당 5회 요청 빈도 제한, 금칙어 정책도 게스트 댓글과 동일(같은 카운터 공유)
                    - 원댓글에만 작성 가능(답글에 답글 불가), 신고된 원댓글에는 작성 불가, 마감된 피드에도 작성 가능
                    - 삭제는 게스트 댓글 삭제 API(`DELETE .../comments/{commentId}/guest`)로 비밀번호를 확인해서 한다
                    - 작성 후 원댓글 작성자 + 이 스레드에 답글을 단 회원들에게 알림이 발송됨
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "답글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용/닉네임/비밀번호 누락(COMMENT_001) / 300자 초과(COMMENT_002) / 금칙어 포함(COMMENT_011) / 답글에 답글(COMMENT_013) / 신고된 원댓글(COMMENT_014)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드(FEED_003) 또는 원댓글(COMMENT_004)을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 빈도 초과(COMMENT_010) / 금칙어 반복 위반으로 일시 차단(COMMENT_012)")
    })
    ApiResponse<CommentCreateResponse> createGuestReply(
            @Parameter(description = "피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "답글을 달 원댓글 id", required = true) @PathVariable Long commentId,
            @Valid @RequestBody CommentCreateRequestGuest request,
            @Parameter(description = "클라이언트 디바이스 식별자(요청 빈도 제한용, 선택)") @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(
            summary = "답글 페이지 원댓글 조회",
            description = """
                    답글 페이지 상단에 고정할 원댓글과 헤더 "댓글 (n)"의 n을 조회한다. 인증 불필요.

                    - totalCount = 원댓글 1 + 답글 수(신고된 답글 제외)
                    - comment.replies는 빈 배열 — 답글 목록은 GET .../comments/{commentId}/replies로 조회
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "원댓글을 찾을 수 없음(COMMENT_004 — 답글 id이거나 신고된 댓글 포함)")
    })
    ApiResponse<CommentThreadResponse> getCommentThread(
            @Parameter(hidden = true) User user,
            @Parameter(description = "피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "원댓글 id", required = true) @PathVariable Long commentId
    );

    @Operation(
            summary = "답글 목록 조회",
            description = "답글 페이지의 답글 목록을 등록순 커서 기반 페이지네이션으로 조회한다. 인증 불필요. 신고된 답글은 제외된다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "원댓글을 찾을 수 없음(COMMENT_004)")
    })
    ApiResponse<CursorPageResponse<CommentReplyResponse>> getReplies(
            @Parameter(hidden = true) User user,
            @Parameter(description = "피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "원댓글 id", required = true) @PathVariable Long commentId,
            @Parameter(description = "커서(마지막으로 받은 답글 id). 첫 페이지는 생략") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (기본 20, 최대 50)") @RequestParam(required = false) Integer size
    );

    @Operation(
            summary = "댓글 공감",
            description = """
                    원댓글에 공감한다. 회원만 가능. 멱등 — 이미 공감한 상태에서 다시 요청해도 200.

                    - 원댓글에만 가능(답글은 공감 불가), 본인 댓글도 가능, 마감된 피드에서도 가능
                    - 응답의 likeCount/isLiked로 화면을 바로 갱신하면 됨
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공감 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "답글에 공감(COMMENT_015) / 신고된 댓글(COMMENT_016)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음(COMMENT_004)")
    })
    ApiResponse<CommentLikeResponse> likeComment(
            @Parameter(hidden = true) User user,
            @Parameter(description = "피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "공감할 원댓글 id", required = true) @PathVariable Long commentId
    );

    @Operation(
            summary = "댓글 공감 취소",
            description = "원댓글 공감을 취소한다. 회원만 가능. 멱등 — 공감하지 않은 상태에서 요청해도 200.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공감 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "답글에 공감 취소(COMMENT_015)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음(COMMENT_004)")
    })
    ApiResponse<CommentLikeResponse> unlikeComment(
            @Parameter(hidden = true) User user,
            @Parameter(description = "피드 id", required = true) @PathVariable Long feedId,
            @Parameter(description = "공감 취소할 원댓글 id", required = true) @PathVariable Long commentId
    );
}
