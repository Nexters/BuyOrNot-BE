package com.nexters.sseotdabwa.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"data", "message", "status", "errorCode"})
public class ApiResponse<T> {

    private final T data;
    private final String message;

    /**
     * 요구 포맷: status 필드는 문자열 "201", "404"
     */
    private final String status;

    /**
     * 에러 응답일 때만 존재
     */
    private final String errorCode;

    public static <T> ApiResponse<T> success(T data, HttpStatus httpStatus) {
        return new ApiResponse<>(data, "성공입니다.", String.valueOf(httpStatus.value()), null);
    }

    public static <T> ApiResponse<T> success(T data, String message, HttpStatus httpStatus) {
        return new ApiResponse<>(data, message, String.valueOf(httpStatus.value()), null);
    }

    public static ApiResponse<Void> success(HttpStatus httpStatus) {
        return new ApiResponse<>(null, "성공입니다.", String.valueOf(httpStatus.value()), null);
    }

    public static ApiResponse<Void> success(String message, HttpStatus httpStatus) {
        return new ApiResponse<>(null, message, String.valueOf(httpStatus.value()), null);
    }

    public static ApiResponse<Void> error(String message, HttpStatus httpStatus, String errorCode) {
        return new ApiResponse<>(null, message, String.valueOf(httpStatus.value()), errorCode);
    }

    public static ApiResponse<Void> error(String message, HttpStatus httpStatus) {
        return new ApiResponse<>(null, message, String.valueOf(httpStatus.value()), null);
    }
}
