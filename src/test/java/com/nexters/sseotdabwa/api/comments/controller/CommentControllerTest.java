package com.nexters.sseotdabwa.api.comments.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

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
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Test
    @DisplayName("회원 댓글 작성 성공 - 201 Created (투표 여부와 무관)")
    void createComment_success_201() throws Exception {
        // given
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(commenter.getId());
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
    @DisplayName("게스트 댓글 작성 성공 - 201 Created (인증 불필요, 비밀번호 포함)")
    void createGuestComment_success_201() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateRequestGuest request = new CommentCreateRequestGuest("저도 고민되네요", "지름신들린수달_1234", "guest-password");

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments/guest")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("댓글 작성 - 계정 기준 분당 5회 초과 시 429 COMMENT_010")
    void createComment_rateLimitExceeded_429() throws Exception {
        // given
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(commenter.getId());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentCreateRequest("댓글 " + i))))
                    .andExpect(status().isCreated());
        }

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("6번째 댓글"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_010"));
    }

    @Test
    @DisplayName("댓글 작성 - 같은 IP를 공유해도(Cloudflare Worker 프록시 상황) 회원별로 독립적으로 카운트된다")
    void createComment_rateLimit_isIndependentPerAccount_evenBehindSharedProxyIp() throws Exception {
        // given: 두 회원이 같은 IP(공유 프록시 상황 재현)에서 요청하지만, 회원 rate limit은 IP를 아예 보지 않는다
        String sharedIp = "203.0.113.1";
        User owner = createUser();
        User memberA = createUser();
        User memberB = createUser();
        Feed feed = createFeed(owner);
        String tokenA = jwtTokenService.createAccessToken(memberA.getId());
        String tokenB = jwtTokenService.createAccessToken(memberB.getId());

        // when: memberA가 분당 한도(5회)를 전부 소진해도
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                            .header("Authorization", "Bearer " + tokenA)
                            .header("X-Forwarded-For", sharedIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentCreateRequest("A의 댓글 " + i))))
                    .andExpect(status().isCreated());
        }

        // then: 같은 IP를 쓰는 memberB는 영향받지 않고 정상적으로 댓글을 작성할 수 있다
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .header("X-Forwarded-For", sharedIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("B의 댓글"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("댓글 작성 - 금칙어 포함 시 400 COMMENT_011")
    void createComment_profanity_400() throws Exception {
        // given
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(commenter.getId());
        CommentCreateRequest request = new CommentCreateRequest("시발 진짜 별로다");

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_011"));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 200 OK (인증 불필요, sort 파라미터 포함)")
    void getComments_success_200() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);

        // when & then
        mockMvc.perform(get("/api/v1/feeds/" + feed.getId() + "/comments").param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("본인 댓글 삭제 성공 - 200 OK")
    void deleteComment_success_200() throws Exception {
        // given
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(commenter.getId());
        CommentCreateResponse created = createCommentDirectly(feed, commenter, token);

        // when & then
        mockMvc.perform(delete("/api/v1/feeds/" + feed.getId() + "/comments/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("게스트 댓글 삭제 성공 - 200 OK (비밀번호 일치)")
    void deleteGuestComment_success_200() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateRequestGuest createRequest = new CommentCreateRequestGuest("내용", "지름신들린수달_1234", "guest-password");
        String body = mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments/guest")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        Long commentId = objectMapper.readTree(body).path("data").path("id").asLong();

        // when & then
        mockMvc.perform(delete("/api/v1/feeds/" + feed.getId() + "/comments/" + commentId + "/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentGuestDeleteRequest("guest-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("게스트 댓글 삭제 - 비밀번호 불일치 시 403 COMMENT_007")
    void deleteGuestComment_wrongPassword_403() throws Exception {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateRequestGuest createRequest = new CommentCreateRequestGuest("내용", "지름신들린수달_1234", "guest-password");
        String body = mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments/guest")
                        .header("X-Forwarded-For", randomIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();
        Long commentId = objectMapper.readTree(body).path("data").path("id").asLong();

        // when & then
        mockMvc.perform(delete("/api/v1/feeds/" + feed.getId() + "/comments/" + commentId + "/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentGuestDeleteRequest("wrong-password"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_007"));
    }

    @Test
    @DisplayName("댓글 신고 성공 - 200 OK (게스트도 인증 없이 가능)")
    void reportComment_success_200() throws Exception {
        // given
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        String token = jwtTokenService.createAccessToken(commenter.getId());
        CommentCreateResponse created = createCommentDirectly(feed, commenter, token);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments/" + created.id() + "/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    // ===== Helper Methods =====

    private CommentCreateResponse createCommentDirectly(Feed feed, User commenter, String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("내용"))))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).path("data").path("id").asLong();
        return new CommentCreateResponse(id);
    }

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

    /**
     * rate limit 카운터가 테스트 간에 공유되지 않도록 테스트마다 고유한 식별자를 사용한다.
     */
    private String randomIp() {
        return "ip-" + UUID.randomUUID();
    }
}
