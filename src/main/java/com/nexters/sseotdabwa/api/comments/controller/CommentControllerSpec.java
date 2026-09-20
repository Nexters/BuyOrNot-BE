package com.nexters.sseotdabwa.api.comments.controller;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
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

@Tag(name = "Comments", description = "댓글(의견) API")
public interface CommentControllerSpec {

    @Operation(
            summary = "회원 댓글 작성",
            description = "투표한 회원이 피드에 댓글(의견)을 남깁니다. 투표 여부는 서버가 재검증합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "댓글 작성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "내용 누락/글자수 초과 / 마감된 피드"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "투표하지 않은 피드"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<CommentCreateResponse> createComment(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequest request
    );

    @Operation(
            summary = "게스트 댓글 작성",
            description = "투표한 게스트가 피드에 댓글(의견)을 남깁니다. "
                    + "투표 응답으로 받은 voteToken으로 투표 여부를 증명합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "댓글 작성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "내용 누락/글자수 초과 / 마감된 피드"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "voteToken이 유효하지 않거나 만료됨"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<CommentCreateResponse> createGuestComment(
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequestGuest request
    );

    @Operation(
            summary = "댓글 목록 조회",
            description = "피드의 댓글(의견) 목록을 등록순으로 커서 기반 페이지네이션 조회합니다. 인증 불필요."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<CursorPageResponse<CommentResponse>> getComments(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    );
}
