package com.nexters.sseotdabwa.api.prelaunch.controller;

import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailRequest;
import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Pre-Launch", description = "사전 등록 API")
public interface PreLaunchControllerSpec {

    @Operation(
            summary = "사전 등록 이메일 등록",
            description = "앱 런칭 전 사전 등록 이메일을 수집합니다. 인증 없이 접근 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "이메일 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 이메일 형식 또는 이메일 미입력"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 등록된 이메일"
            )
    })
    ApiResponse<PreLaunchEmailResponse> registerEmail(
            @Valid @RequestBody PreLaunchEmailRequest request
    );
}
