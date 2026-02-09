package com.nexters.sseotdabwa.api.uploads.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 업로드 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum UploadErrorCode implements ErrorCode {

    UPLOAD_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "UPLOAD_001", "업로드 요청이 올바르지 않습니다."),
    PRESIGN_FAILED(HttpStatus.BAD_GATEWAY, "UPLOAD_002", "Presigned URL 발급에 실패했습니다."),
    S3_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "UPLOAD_003", "S3 객체 삭제에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
