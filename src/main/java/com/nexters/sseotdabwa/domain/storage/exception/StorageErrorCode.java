package com.nexters.sseotdabwa.domain.storage.exception;

import com.nexters.sseotdabwa.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Storage(Domain) 레이어 에러 코드
 * - domain 레이어가 api 레이어 UploadErrorCode를 참조하지 않도록 분리
 */
@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {

    CONTENT_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "S3_001", "contentType은 필수입니다."),
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "S3_002", "허용되지 않은 contentType 입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
