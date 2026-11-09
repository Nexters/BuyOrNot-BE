package com.nexters.sseotdabwa.api.feeds.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.ai.AiDraftGenerator;
import com.nexters.sseotdabwa.domain.feeds.ai.AiFeedDraft;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedAiDraftControllerTest {

    private static final String URL = "/api/v1/feeds/ai-draft";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private UserRepository userRepository;

    @MockBean private AiDraftGenerator aiDraftGenerator;

    @Test
    @DisplayName("회원 - 초안 생성 성공 200")
    void createAiDraft_success() throws Exception {
        when(aiDraftGenerator.generate(anyList()))
                .thenReturn(new AiFeedDraft("먼작귀 가습기 살까요??ㅜㅜ", "디자인 너무 귀여운데 가격대가 좀 있어 보여서 고민중..", FeedCategory.INTERIOR, 59000L));

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenOf(createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/abc_humidifier.jpg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("먼작귀 가습기 살까요??ㅜㅜ"))
                .andExpect(jsonPath("$.data.content").value("디자인 너무 귀여운데 가격대가 좀 있어 보여서 고민중.."))
                .andExpect(jsonPath("$.data.category").value("INTERIOR"))
                .andExpect(jsonPath("$.data.price").value(59000));
    }

    @Test
    @DisplayName("비로그인 - 401")
    void createAiDraft_unauthenticated() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/a.jpg"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이미지 0장 / 4장 이상 - 400")
    void createAiDraft_invalidImageCount() throws Exception {
        String token = tokenOf(createUser());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/a.jpg", "feeds/b.jpg", "feeds/c.jpg", "feeds/d.jpg"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("feeds/ 경로가 아닌 key - 400 FEED_014")
    void createAiDraft_invalidKey() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenOf(createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("https://evil.com/a.jpg"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FEED_014"));
    }

    @Test
    @DisplayName("AI 생성 실패 - 503 FEED_017")
    void createAiDraft_generatorFailure() throws Exception {
        when(aiDraftGenerator.generate(anyList())).thenThrow(new GlobalException(FeedErrorCode.FEED_AI_DRAFT_FAILED));

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + tokenOf(createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/a.jpg"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("FEED_017"));
    }

    @Test
    @DisplayName("같은 계정 분당 5회 초과 - 429 FEED_016")
    void createAiDraft_rateLimited() throws Exception {
        when(aiDraftGenerator.generate(anyList())).thenReturn(new AiFeedDraft("t", "c", FeedCategory.ETC, null));
        String token = tokenOf(createUser());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(URL)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(List.of("feeds/a.jpg"))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/a.jpg"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("FEED_016"));
    }

    @Test
    @DisplayName("비회원 - 인증 없이 초안 생성 200")
    void createGuestAiDraft_success() throws Exception {
        when(aiDraftGenerator.generate(anyList())).thenReturn(new AiFeedDraft("후드집업 이거 어때", "c", FeedCategory.FASHION, 0L));

        mockMvc.perform(post(URL + "/guest")
                        .header("X-Forwarded-For", randomIp())
                        .header("X-Device-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/a.jpg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("후드집업 이거 어때"))
                .andExpect(jsonPath("$.data.category").value("FASHION"))
                .andExpect(jsonPath("$.data.price").doesNotExist());
    }

    @Test
    @DisplayName("비회원 - 같은 IP에서 분당 5회 초과 시 429 FEED_016")
    void createGuestAiDraft_rateLimited() throws Exception {
        when(aiDraftGenerator.generate(anyList())).thenReturn(new AiFeedDraft("t", "c", FeedCategory.ETC, 0L));
        String ip = randomIp();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(URL + "/guest")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(List.of("feeds/a.jpg"))))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post(URL + "/guest")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("feeds/a.jpg"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("FEED_016"));
    }

    @Test
    @DisplayName("비회원 - feeds/ 경로가 아닌 key는 400 FEED_014")
    void createGuestAiDraft_invalidKey() throws Exception {
        mockMvc.perform(post(URL + "/guest")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of("profiles/a.jpg"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FEED_014"));
    }

    private String randomIp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
    }

    private String body(List<String> keys) throws Exception {
        return objectMapper.writeValueAsString(Map.of("s3ObjectKeys", keys));
    }

    private String tokenOf(User user) {
        return jwtTokenService.createAccessToken(user.getId());
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }
}
