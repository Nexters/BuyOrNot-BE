package com.nexters.sseotdabwa.api.uploads.facade;

import com.nexters.sseotdabwa.api.uploads.dto.CreatePresignedPutRequest;
import com.nexters.sseotdabwa.api.uploads.dto.PresignedPutResponse;
import com.nexters.sseotdabwa.domain.storage.service.S3StorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * uploads 관련 흐름을 조합하는 Facade
 */
@Component
@RequiredArgsConstructor
public class UploadFacade {

    private final S3StorageService s3StorageService;

    /**
     * presigned PUT 발급
     */
    public PresignedPutResponse createPresignedPut(CreatePresignedPutRequest request) {
        var result = s3StorageService.createPresignedPut(request.fileName(), request.contentType());
        return new PresignedPutResponse(result.uploadUrl(), result.s3Key(), result.viewUrl());
    }

    /**
     * 객체 삭제
     * - 보통은 피드 삭제 등 도메인 로직에서 호출
     */
    @Transactional
    public void deleteObject(String s3ObjectKey) {
        s3StorageService.deleteObject(s3ObjectKey);
    }
}
