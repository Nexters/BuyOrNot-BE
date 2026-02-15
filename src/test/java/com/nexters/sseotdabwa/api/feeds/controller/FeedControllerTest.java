package com.nexters.sseotdabwa.api.feeds.controller;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequest;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedImageRepository feedImageRepository;

    // ===== 피드 등록 =====

    @Test
    @DisplayName("피드 등록 성공 - 201 Created 및 feedId 반환")
    void createFeed_success() throws Exception {
        // given
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.valueOf("FOOD"),
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                "feeds/uuid_test.jpg",
                1080,
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedId").exists())
                .andExpect(jsonPath("$.status").value("201"));
    }

    @Test
    @DisplayName("피드 등록 실패 - 인증 없으면 401")
    void createFeed_unauthorized() throws Exception {
        // given
        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.valueOf("FOOD"),
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                "feeds/uuid_test.jpg",
                1080,
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("피드 등록 실패 - s3ObjectKey blank면 400")
    void createFeed_invalid_s3ObjectKey_blank_returns400() throws Exception {
        // given
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.valueOf("FOOD"),
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                "   ", // @NotBlank
                1080,
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("피드 등록 실패 - content 100자 초과면 400")
    void createFeed_invalid_content_tooLong_returns400() throws Exception {
        // given
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = jwtTokenService.createAccessToken(user.getId());

        String longContent = "a".repeat(101);

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.valueOf("FOOD"),
                8000L,
                longContent, // @Size(max=100)
                "feeds/uuid_test.jpg",
                1080,
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("피드 등록 실패 - imageWidth 0이면 400")
    void createFeed_invalid_imageWidth_returns400() throws Exception {
        // given
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.valueOf("FOOD"),
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                "feeds/uuid_test.jpg",
                0,      // @Positive
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    // ===== 피드 리스트 조회 =====

    @Test
    @DisplayName("피드 리스트 조회 성공 - 200 OK")
    void getFeedList_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data[0].author.userId").value(user.getId()));
    }

    @Test
    @DisplayName("피드 리스트 조회 성공 - 비로그인 유저도 접근 가능")
    void getFeedList_noAuth_success() throws Exception {
        // given
        User user = createUser();
        createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ===== 피드 삭제 =====

    @Test
    @DisplayName("피드 삭제 성공 - 본인 피드 200 OK")
    void deleteFeed_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(delete("/api/v1/feeds/" + feed.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("피드 삭제 실패 - 존재하지 않는 피드 404")
    void deleteFeed_notFound_404() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(delete("/api/v1/feeds/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FEED_003"));
    }

    @Test
    @DisplayName("피드 삭제 실패 - 타인 피드 403")
    void deleteFeed_forbidden_403() throws Exception {
        // given
        User owner = createUser();
        User otherUser = createUser();
        String otherToken = jwtTokenService.createAccessToken(otherUser.getId());
        Feed feed = createFeedWithImage(owner);

        // when & then
        mockMvc.perform(delete("/api/v1/feeds/" + feed.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FEED_004"));
    }

    @Test
    @DisplayName("피드 삭제 실패 - 비로그인 401")
    void deleteFeed_unauthorized_401() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/feeds/1"))
                .andExpect(status().isUnauthorized());
    }

    // ===== Helper Methods =====

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private Feed createFeedWithImage(User user) {
        Feed feed = feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build());

        feedImageRepository.save(FeedImage.builder()
                .feed(feed)
                .s3ObjectKey("feeds/test_" + UUID.randomUUID() + ".jpg")
                .build());

        return feed;
    }
}
