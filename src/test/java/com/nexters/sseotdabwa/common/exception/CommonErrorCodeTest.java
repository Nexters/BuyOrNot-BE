package com.nexters.sseotdabwa.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CommonErrorCodeTest {

    @Test
    @DisplayName("CommonErrorCode는 ErrorCode 인터페이스를 구현한다")
    void commonErrorCode_implements_errorCode() {
        // given
        CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;

        // then
        assertThat(errorCode).isInstanceOf(ErrorCode.class);
    }

    @Test
    @DisplayName("BAD_REQUEST는 400 상태 코드를 가진다")
    void badRequest_has_400_status() {
        // given
        CommonErrorCode errorCode = CommonErrorCode.BAD_REQUEST;

        // then
        assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorCode.getCode()).isEqualTo("COMMON_400");
        assertThat(errorCode.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("UNAUTHORIZED는 401 상태 코드를 가진다")
    void unauthorized_has_401_status() {
        // given
        CommonErrorCode errorCode = CommonErrorCode.UNAUTHORIZED;

        // then
        assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorCode.getCode()).isEqualTo("COMMON_401");
        assertThat(errorCode.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("FORBIDDEN은 403 상태 코드를 가진다")
    void forbidden_has_403_status() {
        // given
        CommonErrorCode errorCode = CommonErrorCode.FORBIDDEN;

        // then
        assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorCode.getCode()).isEqualTo("COMMON_403");
        assertThat(errorCode.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("NOT_FOUND는 404 상태 코드를 가진다")
    void notFound_has_404_status() {
        // given
        CommonErrorCode errorCode = CommonErrorCode.NOT_FOUND;

        // then
        assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(errorCode.getCode()).isEqualTo("COMMON_404");
        assertThat(errorCode.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("INTERNAL_SERVER_ERROR는 500 상태 코드를 가진다")
    void internalServerError_has_500_status() {
        // given
        CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

        // then
        assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorCode.getCode()).isEqualTo("COMMON_500");
        assertThat(errorCode.getMessage()).isNotBlank();
    }
}
