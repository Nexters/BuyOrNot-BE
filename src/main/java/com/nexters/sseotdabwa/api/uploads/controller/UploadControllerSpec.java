package com.nexters.sseotdabwa.api.uploads.controller;

import com.nexters.sseotdabwa.api.uploads.dto.CreatePresignedPutRequest;
import com.nexters.sseotdabwa.api.uploads.dto.DeleteObjectRequest;
import com.nexters.sseotdabwa.api.uploads.dto.PresignedPutResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Uploads", description = "이미지 업로드(Presigned URL) API")
public interface UploadControllerSpec {

    @Operation(
            summary = "Presigned PUT URL 발급",
            description = """
                    클라이언트(Android/iOS/Web)가 S3에 직접 PUT 업로드할 수 있도록 Presigned URL을 발급합니다.

                    **동작 흐름**
                    1) 클라이언트가 파일 메타정보(파일명/확장자 또는 contentType 등)로 Presigned URL 발급 요청
                    2) 서버가 S3 Object Key 생성 후 Presigned PUT URL(uploadUrl) 발급
                    3) 클라이언트가 uploadUrl로 S3에 PUT 업로드
                    4) 클라이언트는 업로드 완료 후, s3ObjectKey를 전달

                    **중요 정책/주의사항**
                    - 서버는 파일을 직접 받지 않습니다.
                    - 클라이언트는 응답받은 uploadUrl로 HTTP PUT 업로드합니다.
                    - 업로드 시 Content-Type 헤더는 요청의 contentType과 동일해야 합니다.
                    - viewUrl은 CloudFront public 도메인 기반의 조회 URL이며, 앱에서 이미지 표시 시 사용합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Presigned URL 발급 성공",
                    content = @Content(schema = @Schema(implementation = PresignedPutResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (필수값 누락/정책 위반: 지원하지 않는 contentType 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 (Presigner 생성/서명 실패 등)"
            )
    })
    ApiResponse<PresignedPutResponse> createPresignedPut(
            @Valid @RequestBody CreatePresignedPutRequest request
    );

    @Operation(
            summary = "S3 객체 삭제",
            description = """
                    s3Key에 해당하는 S3 객체를 삭제합니다.

                    - 운영 환경에서는 클라이언트에 직접 노출하기보다,
                      '피드 삭제/수정' 같은 도메인 로직에서 내부적으로 호출하는 구성이 더 안전합니다.
                    - 존재하지 않는 객체를 삭제하더라도(정책에 따라) 성공처럼 처리될 수 있습니다.

                    **사용 예**
                    - 업로드는 했지만 피드 등록을 취소한 경우
                    - 도메인 데이터 삭제 시 이미지 정리
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 (s3Key 누락/형식 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "대상이 존재하지 않음 (정책에 따라 200으로 처리할 수도 있음)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 (S3 삭제 실패 등)"
            )
    })
    ApiResponse<Void> deleteObject(
            @Valid @RequestBody DeleteObjectRequest request
    );
}
