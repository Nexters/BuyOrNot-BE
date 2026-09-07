package com.nexters.sseotdabwa.api.guest.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("게스트 닉네임 발급 성공 - 인증 없이 유효한 형식의 닉네임 반환")
    void issueNickname_success() throws Exception {
        mockMvc.perform(get("/api/v1/guest/nickname"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value(matchesPattern("^.+_\\d{4}$")));
    }
}
