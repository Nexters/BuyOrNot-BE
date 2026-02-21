package com.nexters.sseotdabwa.api.prelaunch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailRequest;
import com.nexters.sseotdabwa.domain.prelaunch.entity.PreLaunchEmail;
import com.nexters.sseotdabwa.domain.prelaunch.repository.PreLaunchEmailRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PreLaunchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PreLaunchEmailRepository preLaunchEmailRepository;

    @Test
    @DisplayName("이메일 등록 성공 - 201 Created")
    void registerEmail_success_201() throws Exception {
        // given
        PreLaunchEmailRequest request = new PreLaunchEmailRequest("test@example.com");

        // when & then
        mockMvc.perform(post("/api/v1/pre-launch/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("중복 이메일 등록 시 409 - PRELAUNCH_001")
    void registerEmail_duplicate_409() throws Exception {
        // given
        preLaunchEmailRepository.save(PreLaunchEmail.builder().email("duplicate@example.com").build());
        PreLaunchEmailRequest request = new PreLaunchEmailRequest("duplicate@example.com");

        // when & then
        mockMvc.perform(post("/api/v1/pre-launch/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRELAUNCH_001"));
    }

    @Test
    @DisplayName("이메일 미입력 시 400 - 검증 실패")
    void registerEmail_blank_400() throws Exception {
        // given
        String requestBody = "{\"email\": \"\"}";

        // when & then
        mockMvc.perform(post("/api/v1/pre-launch/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("잘못된 이메일 형식 시 400 - 검증 실패")
    void registerEmail_invalidFormat_400() throws Exception {
        // given
        PreLaunchEmailRequest request = new PreLaunchEmailRequest("invalid-email");

        // when & then
        mockMvc.perform(post("/api/v1/pre-launch/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("인증 없이 접근 가능")
    void registerEmail_noAuth_accessible() throws Exception {
        // given
        PreLaunchEmailRequest request = new PreLaunchEmailRequest("noauth@example.com");

        // when & then
        mockMvc.perform(post("/api/v1/pre-launch/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
