package com.nexters.sseotdabwa.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GlobalExceptionTest {

    @Test
    @DisplayName("GlobalException은 RuntimeException을 상속한다")
    void globalException_extends_runtimeException() {
        // given
        GlobalException exception = new GlobalException(CommonErrorCode.BAD_REQUEST);

        // then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("GlobalException은 ErrorCode를 가진다")
    void globalException_has_errorCode() {
        // given
        ErrorCode errorCode = CommonErrorCode.BAD_REQUEST;

        // when
        GlobalException exception = new GlobalException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
    }

    @Test
    @DisplayName("GlobalException의 메시지는 ErrorCode의 메시지와 같다")
    void globalException_message_equals_errorCode_message() {
        // given
        ErrorCode errorCode = CommonErrorCode.NOT_FOUND;

        // when
        GlobalException exception = new GlobalException(errorCode);

        // then
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }

    @Test
    @DisplayName("GlobalException은 커스텀 메시지를 설정할 수 있다")
    void globalException_can_have_custom_message() {
        // given
        ErrorCode errorCode = CommonErrorCode.BAD_REQUEST;
        String customMessage = "사용자 ID는 필수입니다.";

        // when
        GlobalException exception = new GlobalException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }
}
