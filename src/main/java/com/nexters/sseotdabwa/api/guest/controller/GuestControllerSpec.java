package com.nexters.sseotdabwa.api.guest.controller;

import com.nexters.sseotdabwa.api.guest.dto.GuestNicknameResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Guest", description = "비회원(게스트) API")
public interface GuestControllerSpec {

    @Operation(
            summary = "게스트 랜덤 닉네임 발급",
            description = """
                    비회원 피드 작성 화면에서 사용할 랜덤 닉네임을 발급합니다.

                    - 인증 불필요
                    - 호출할 때마다 새로운 닉네임을 랜덤으로 발급 (서버는 발급 이력을 저장하지 않음)
                    - 재발급(교체) 시에도 동일하게 이 API를 다시 호출하면 됨
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "닉네임 발급 성공"
            )
    })
    ApiResponse<GuestNicknameResponse> issueNickname();
}
