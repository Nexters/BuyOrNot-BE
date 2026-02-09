package com.nexters.sseotdabwa.api.uploads.controller;

import com.nexters.sseotdabwa.api.uploads.dto.CreatePresignedPutRequest;
import com.nexters.sseotdabwa.api.uploads.dto.DeleteObjectRequest;
import com.nexters.sseotdabwa.api.uploads.dto.PresignedPutResponse;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Uploads", description = "이미지 업로드(Presigned URL) API")
public interface UploadControllerSpec {

    @Operation(
            summary = "Presigned PUT URL 발급",
            description = """
                    클라이언트(Android/iOS/Web)가 S3에 직접 PUT 업로드할 수 있도록 Presigned URL을 발급합니다.

                    - 서버는 파일을 직접 받지 않습니다.
                    - 클라이언트는 응답받은 uploadUrl로 PUT 업로드합니다.
                    - 업로드 시 Content-Type 헤더는 요청(contentType)과 동일해야 합니다.
                    - viewUrl은 CloudFront public 도메인 기반 조회 URL입니다.
                    """
    )
    ApiResponse<PresignedPutResponse> createPresignedPut(@Valid @RequestBody CreatePresignedPutRequest request);

    @Operation(
            summary = "S3 객체 삭제",
            description = """
                    s3Key에 해당하는 S3 객체를 삭제합니다.
                    - 운영에선 피드 삭제 같은 도메인 로직에서 내부적으로 호출하는 구성이 더 안전합니다.
                    """
    )
    ApiResponse<Void> deleteObject(@Valid @RequestBody DeleteObjectRequest request);
}
