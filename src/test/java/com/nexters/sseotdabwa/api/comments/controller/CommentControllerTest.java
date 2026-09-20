package com.nexters.sseotdabwa.api.comments.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.dto.VoteResponse;
import com.nexters.sseotdabwa.api.votes.facade.VoteFacade;
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
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private VoteFacade voteFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private VoteLogRepository voteLogRepository;

    @Test
    @DisplayName("회원 댓글 작성 성공 - 201 Created")
    void createComment_success_201() throws Exception {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(voter.getId());
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());
        CommentCreateRequest request = new CommentCreateRequest("저도 고민되네요");

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("회원 댓글 작성 실패 - 비인증 401")
    void createComment_unauthorized_401() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateRequest request = new CommentCreateRequest("내용");

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("투표하지 않은 회원 댓글 작성 실패 - 403 COMMENT_004")
    void createComment_notVoted_403() throws Exception {
        // given
        User owner = createUser();
        User notVoter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(notVoter.getId());
        CommentCreateRequest request = new CommentCreateRequest("내용");

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_004"));
    }

    @Test
    @DisplayName("게스트 댓글 작성 성공 - 201 Created (인증 불필요)")
    void createGuestComment_success_201() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteResponse voteResponse = voteFacade.guestVote(feed.getId(), new VoteRequest(VoteChoice.YES));
        CommentCreateRequestGuest request = new CommentCreateRequestGuest("저도 고민되네요", "지름신들린수달_1234", voteResponse.voteToken());

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("voteToken 없이 게스트 댓글 작성 실패 - 403 COMMENT_005")
    void createGuestComment_invalidVoteToken_403() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateRequestGuest request = new CommentCreateRequestGuest("내용", "지름신들린수달_1234", "invalid-token");

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_005"));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 200 OK (인증 불필요)")
    void getComments_success_200() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);

        // when & then
        mockMvc.perform(get("/api/v1/feeds/" + feed.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.content").isArray());
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
                .build());
    }
}
