package com.nexters.sseotdabwa.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
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
    public ResponseEntity<ErrorResponse> handleGlobalException(
            GlobalException exception,
            HttpServletRequest request
    ) {
        log.warn("GlobalException: {} - {}", exception.getErrorCode().getCode(), exception.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(exception, request.getRequestURI());

        return ResponseEntity
                .status(exception.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = extractFieldErrorMessage(exception);
        log.warn("MethodArgumentNotValidException: {}", message);

        ErrorResponse errorResponse = ErrorResponse.of(
                LocalDateTime.now(),
                CommonErrorCode.BAD_REQUEST.getHttpStatus().value(),
                CommonErrorCode.BAD_REQUEST.getCode(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(CommonErrorCode.BAD_REQUEST.getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException exception,
            HttpServletRequest request
    ) {
        String message = extractBindingErrorMessage(exception);
        log.warn("BindException: {}", message);

        ErrorResponse errorResponse = ErrorResponse.of(
                LocalDateTime.now(),
                CommonErrorCode.BAD_REQUEST.getHttpStatus().value(),
                CommonErrorCode.BAD_REQUEST.getCode(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(CommonErrorCode.BAD_REQUEST.getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected Exception: ", exception);

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(errorResponse);
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
