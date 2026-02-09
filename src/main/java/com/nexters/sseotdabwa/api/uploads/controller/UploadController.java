package com.nexters.sseotdabwa.api.uploads.controller;

import com.nexters.sseotdabwa.api.uploads.dto.CreatePresignedPutRequest;
import com.nexters.sseotdabwa.api.uploads.dto.DeleteObjectRequest;
import com.nexters.sseotdabwa.api.uploads.dto.PresignedPutResponse;
import com.nexters.sseotdabwa.api.uploads.facade.UploadFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 이미지 업로드 API 컨트롤러
 * - Presigned URL 발급
 * - 이미지 삭제
 */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController implements UploadControllerSpec {

    private final UploadFacade uploadFacade;

    @PostMapping("/presigned-put")
    @Override
    public ApiResponse<PresignedPutResponse> createPresignedPut(@RequestBody CreatePresignedPutRequest request) {
        PresignedPutResponse response = uploadFacade.createPresignedPut(request);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @DeleteMapping("/object")
    @Override
    public ApiResponse<Void> deleteObject(@RequestBody DeleteObjectRequest request) {
        uploadFacade.deleteObject(request.s3ObjectKey());
        return ApiResponse.success(null, HttpStatus.NO_CONTENT);
    }
}
