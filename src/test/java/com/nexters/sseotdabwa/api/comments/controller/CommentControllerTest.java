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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
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
}
