package com.nexters.sseotdabwa.api.uploads.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * presigned PUT URL 발급 요청
 * - fileName: 원본 파일명
 * - contentType: 업로드 시 사용할 MIME 타입(ex: image/jpeg)
 */
public record CreatePresignedPutRequest(
        @NotBlank(message = "fileName은 필수입니다.")
        String fileName,

        @NotBlank(message = "contentType은 필수입니다.")
        String contentType
) {}
