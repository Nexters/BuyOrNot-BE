package com.nexters.sseotdabwa.api.votes.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VoteControllerTest {

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
    private VoteLogRepository voteLogRepository;

    // ===== 회원 투표 =====

    @Test
    @DisplayName("회원 투표 성공 - 201 Created")
    void vote_success_201() throws Exception {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(voter.getId());
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.choice").value("YES"))
                .andExpect(jsonPath("$.data.yesCount").value(1))
                .andExpect(jsonPath("$.data.noCount").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("회원 투표 실패 - 비인증 401")
    void vote_unauthorized_401() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원 중복 투표 시 400 - VOTE_001")
    void vote_duplicate_400() throws Exception {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(voter.getId());
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VOTE_001"));
    }

    @Test
    @DisplayName("본인 피드 투표 시 400 - VOTE_003")
    void vote_ownFeed_400() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(owner.getId());
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VOTE_003"));
    }

    // ===== 게스트 투표 =====

    @Test
    @DisplayName("게스트 투표 성공 - 201 Created")
    void guestVote_success_201() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteRequest request = new VoteRequest(VoteChoice.NO);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/votes/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.choice").value("NO"))
                .andExpect(jsonPath("$.data.yesCount").value(0))
                .andExpect(jsonPath("$.data.noCount").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("게스트 마감 피드 투표 시 400 - VOTE_002")
    void guestVote_closedFeed_400() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        feed.closeVote();
        feedRepository.save(feed);
        VoteRequest request = new VoteRequest(VoteChoice.YES);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/votes/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VOTE_002"));
    }

    // ===== Helper Methods =====

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private Feed createFeed(User user) {
        return feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build());
    }
}
