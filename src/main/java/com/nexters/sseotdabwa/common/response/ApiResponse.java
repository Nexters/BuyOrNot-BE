package com.nexters.sseotdabwa.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"data", "message", "status"})
public class ApiResponse<T> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    private final String message;

    /**
     * 요구 포맷: status 필드는 문자열
     * (SUCCESS/ERROR 등)
     */
    private final String status;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "성공입니다.", ApiStatus.SUCCESS.getValue());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, message, ApiStatus.SUCCESS.getValue());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null, "성공입니다.", ApiStatus.SUCCESS.getValue());
    }

    /**
     * 에러 응답은 나중에 GlobalExceptionHandler에서 통일해서 사용
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, message, ApiStatus.ERROR.getValue());
    }
}
