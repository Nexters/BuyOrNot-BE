package com.nexters.sseotdabwa.api.uploads.controller;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.sseotdabwa.api.uploads.dto.CreatePresignedPutRequest;
import com.nexters.sseotdabwa.api.uploads.facade.UploadFacade;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private UploadFacade uploadFacade;

    @Test
    @DisplayName("Presigned PUT 발급 성공 - 인증 사용자만 가능")
    void createPresignedPut_success() throws Exception {
        // given: 로그인 사용자 + JWT
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = jwtTokenService.createAccessToken(user.getId());

        // facade 반환 mocking
        given(uploadFacade.createPresignedPut(any()))
                .willReturn(new com.nexters.sseotdabwa.api.uploads.dto.PresignedPutResponse(
                        "https://s3.example.com/presigned-put-url",
                        "feeds/uuid_test.jpg",
                        "https://d123.cloudfront.net/feeds/uuid_test.jpg"
                ));

        CreatePresignedPutRequest request = new CreatePresignedPutRequest(
                "test.jpg",
                "image/jpeg"
        );

        // when & then
        mockMvc.perform(post("/api/v1/uploads/presigned-put")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").exists())
                .andExpect(jsonPath("$.data.s3ObjectKey").exists())
                .andExpect(jsonPath("$.data.viewUrl").exists())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("Presigned PUT 발급 실패 - Authorization 없으면 401")
    void createPresignedPut_unauthorized() throws Exception {
        // given
        CreatePresignedPutRequest request = new CreatePresignedPutRequest(
                "test.jpg",
                "image/jpeg"
        );

        // when & then
        mockMvc.perform(post("/api/v1/uploads/presigned-put")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
