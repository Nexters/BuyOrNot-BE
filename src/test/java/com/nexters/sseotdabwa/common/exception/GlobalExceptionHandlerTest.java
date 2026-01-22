package com.nexters.sseotdabwa.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GlobalException 발생 시 ErrorResponse 형식으로 응답한다")
    void handleGlobalException_returns_errorResponse() throws Exception {
        mockMvc.perform(get("/test/global-exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.path").value("/test/global-exception"));
    }

    @Test
    @DisplayName("커스텀 메시지가 있는 GlobalException 발생 시 커스텀 메시지를 반환한다")
    void handleGlobalException_returns_custom_message() throws Exception {
        mockMvc.perform(get("/test/custom-message")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMMON_404"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/test/custom-message"));
    }

    @Test
    @DisplayName("일반 Exception 발생 시 500 에러를 반환한다")
    void handleException_returns_internal_server_error() throws Exception {
        mockMvc.perform(get("/test/exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("COMMON_500"))
                .andExpect(jsonPath("$.path").value("/test/exception"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/global-exception")
        public void throwGlobalException() {
            throw new GlobalException(CommonErrorCode.BAD_REQUEST);
        }

        @GetMapping("/test/custom-message")
        public void throwGlobalExceptionWithCustomMessage() {
            throw new GlobalException(CommonErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        @GetMapping("/test/exception")
        public void throwException() {
            throw new RuntimeException("예상치 못한 오류");
        }
    }
}
