package com.nexters.sseotdabwa.api.votes.controller;

import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.dto.VoteResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.domain.users.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Votes", description = "투표 API")
public interface VoteControllerSpec {

    @Operation(
            summary = "회원 투표",
            description = "회원이 피드에 YES/NO 투표합니다. 중복 투표는 불가합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "투표 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 투표함 / 마감된 피드 / 본인 피드"
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
    ApiResponse<VoteResponse> vote(
            @Parameter(hidden = true) User user,
            @PathVariable Long feedId,
            @Valid @RequestBody VoteRequest request
    );

    @Operation(
            summary = "비회원 투표",
            description = "비회원(게스트)이 피드에 YES/NO 투표합니다. 중복 방지는 클라이언트에서 처리합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "투표 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "마감된 피드"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "피드를 찾을 수 없음"
            )
    })
    ApiResponse<VoteResponse> guestVote(
            @PathVariable Long feedId,
            @Valid @RequestBody VoteRequest request
    );
}
