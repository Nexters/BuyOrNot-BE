package com.nexters.sseotdabwa.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import lombok.Setter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @DisplayName("GlobalException 발생 시 ApiResponse 형식으로 응답한다")
    void handleGlobalException_returns_errorResponse() throws Exception {
        mockMvc.perform(get("/test/global-exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("커스텀 메시지가 있는 GlobalException 발생 시 커스텀 메시지를 반환한다")
    void handleGlobalException_returns_custom_message() throws Exception {
        mockMvc.perform(get("/test/custom-message")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.errorCode").value("COMMON_404"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("일반 Exception 발생 시 500 에러를 반환한다")
    void handleException_returns_internal_server_error() throws Exception {
        mockMvc.perform(get("/test/exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.status").value("500"))
                .andExpect(jsonPath("$.errorCode").value("COMMON_500"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 발생 시 400 에러를 반환한다")
    void handleMethodArgumentNotValidException_returns_badRequest() throws Exception {
        String invalidJson = "{\"name\": \"\"}";

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("BindException 발생 시 400 에러를 반환한다")
    void handleBindException_returns_badRequest() throws Exception {
        mockMvc.perform(get("/test/bind-validation")
                        .param("name", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("검증 예외 발생 시 첫 번째 필드 에러 메시지를 반환한다")
    void handleValidationException_returns_first_field_error_message() throws Exception {
        String invalidJson = "{\"name\": \"ab\"}";

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
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

        @PostMapping("/test/validation")
        public void validateRequest(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/bind-validation")
        public void validateBind(@Valid TestRequest request) {
        }
    }

    @Getter
    @Setter
    static class TestRequest {

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 3, max = 20, message = "이름은 3자 이상 20자 이하여야 합니다.")
        private String name;
    }
}
