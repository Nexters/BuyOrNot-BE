package com.nexters.sseotdabwa.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorResponseTest {

    @Test
    @DisplayName("ErrorResponse는 timestamp, status, code, message, path를 가진다")
    void errorResponse_has_required_fields() {
        // given
        LocalDateTime timestamp = LocalDateTime.now();
        int status = HttpStatus.BAD_REQUEST.value();
        String code = "COMMON_400";
        String message = "잘못된 요청입니다.";
        String path = "/api/test";

        // when
        ErrorResponse response = ErrorResponse.of(timestamp, status, code, message, path);

        // then
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("ErrorResponse는 ErrorCode와 path로 생성할 수 있다")
    void errorResponse_can_be_created_from_errorCode() {
        // given
        ErrorCode errorCode = CommonErrorCode.NOT_FOUND;
        String path = "/api/users/123";

        // when
        ErrorResponse response = ErrorResponse.of(errorCode, path);

        // then
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(errorCode.getHttpStatus().value());
        assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.getPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("ErrorResponse는 GlobalException과 path로 생성할 수 있다")
    void errorResponse_can_be_created_from_globalException() {
        // given
        GlobalException exception = new GlobalException(CommonErrorCode.UNAUTHORIZED);
        String path = "/api/secure";

        // when
        ErrorResponse response = ErrorResponse.of(exception, path);

        // then
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getCode()).isEqualTo("COMMON_401");
        assertThat(response.getMessage()).isEqualTo(exception.getMessage());
        assertThat(response.getPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("커스텀 메시지가 있는 GlobalException으로 ErrorResponse를 생성할 수 있다")
    void errorResponse_uses_custom_message_from_globalException() {
        // given
        String customMessage = "사용자 ID가 유효하지 않습니다.";
        GlobalException exception = new GlobalException(CommonErrorCode.BAD_REQUEST, customMessage);
        String path = "/api/users";

        // when
        ErrorResponse response = ErrorResponse.of(exception, path);

        // then
        assertThat(response.getMessage()).isEqualTo(customMessage);
    }
}
