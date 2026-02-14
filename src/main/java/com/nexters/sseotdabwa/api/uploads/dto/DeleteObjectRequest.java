package com.nexters.sseotdabwa.api.uploads.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * S3 객체 삭제 요청
 */
public record DeleteObjectRequest(
        @NotBlank(message = "s3Key는 필수입니다.")
        String s3ObjectKey
) {}
