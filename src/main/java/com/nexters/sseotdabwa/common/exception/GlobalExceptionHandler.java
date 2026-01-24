package com.nexters.sseotdabwa.common.exception;

import com.nexters.sseotdabwa.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(GlobalException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("GlobalException: {} - {}", errorCode.getCode(), exception.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                exception.getMessage(),
                errorCode.getHttpStatus(),
                errorCode.getCode()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        String message = extractFieldErrorMessage(exception);
        log.warn("MethodArgumentNotValidException: {}", message);

        ApiResponse<Void> response = ApiResponse.error(
                message,
                CommonErrorCode.BAD_REQUEST.getHttpStatus(),
                CommonErrorCode.BAD_REQUEST.getCode()
        );

        return ResponseEntity
                .status(CommonErrorCode.BAD_REQUEST.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        String message = extractBindingErrorMessage(exception);
        log.warn("BindException: {}", message);

        ApiResponse<Void> response = ApiResponse.error(
                message,
                CommonErrorCode.BAD_REQUEST.getHttpStatus(),
                CommonErrorCode.BAD_REQUEST.getCode()
        );

        return ResponseEntity
                .status(CommonErrorCode.BAD_REQUEST.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unexpected Exception: ", exception);

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getCode()
        );

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(response);
    }

    private String extractFieldErrorMessage(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null) {
            return fieldError.getDefaultMessage();
        }
        return CommonErrorCode.BAD_REQUEST.getMessage();
    }

    private String extractBindingErrorMessage(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError != null) {
            return fieldError.getDefaultMessage();
        }
        return CommonErrorCode.BAD_REQUEST.getMessage();
    }
}
