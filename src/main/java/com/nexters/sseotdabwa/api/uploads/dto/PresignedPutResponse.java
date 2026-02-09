package com.nexters.sseotdabwa.api.uploads.dto;

/**
 * presigned PUT URL 발급 응답
 * - uploadUrl: 클라이언트가 PUT 업로드할 URL
 * - s3Key: DB에 저장할 key
 * - viewUrl: CloudFront public 조회 URL
 */
public record PresignedPutResponse(
        String uploadUrl,
        String s3Key,
        String viewUrl
) {}
