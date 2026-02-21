package com.nexters.sseotdabwa.api.users.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.auth.entity.RefreshToken;
import com.nexters.sseotdabwa.domain.auth.repository.RefreshTokenRepository;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedReview;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedReviewRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedImageRepository feedImageRepository;

    @Autowired
    private FeedReviewRepository feedReviewRepository;

    @Autowired
    private VoteLogRepository voteLogRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("회원 탈퇴 성공 - 닉네임 반환")
    void withdraw_success_returnsNickname() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 유저 및 관련 데이터 모두 삭제")
    void withdraw_success_deletesAllRelatedData() throws Exception {
        // given
        User user = createUser();
        User otherUser = createUser();
        Feed feed = createFeed(user);
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("key1").build());
        feedReviewRepository.save(FeedReview.builder().feed(feed).content("리뷰").build());
        voteLogRepository.save(VoteLog.builder().user(otherUser).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());

        Feed otherFeed = createFeed(otherUser);
        voteLogRepository.save(VoteLog.builder().user(user).feed(otherFeed).choice(VoteChoice.NO).voteType(VoteType.USER).build());

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // then
        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(feedRepository.findByUserId(user.getId())).isEmpty();
        assertThat(feedImageRepository.count()).isZero();
        assertThat(feedReviewRepository.count()).isZero();
        assertThat(voteLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("회원 탈퇴 시 다른 유저의 데이터는 영향 없음")
    void withdraw_success_doesNotAffectOtherUsers() throws Exception {
        // given
        User user = createUser();
        User otherUser = createUser();
        Feed otherFeed = createFeed(otherUser);
        feedImageRepository.save(FeedImage.builder().feed(otherFeed).s3ObjectKey("other-key").build());

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // then
        assertThat(userRepository.findById(otherUser.getId())).isPresent();
        assertThat(feedRepository.findByUserId(otherUser.getId())).hasSize(1);
        assertThat(feedImageRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 탈퇴 시 Refresh Token도 삭제됨")
    void withdraw_success_deletesRefreshTokens() throws Exception {
        // given
        User user = createUser();
        User otherUser = createUser();

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token("user-refresh-token-1")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token("user-refresh-token-2")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(otherUser.getId())
                .token("other-user-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build());

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // then
        assertThat(refreshTokenRepository.findByToken("user-refresh-token-1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("user-refresh-token-2")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("other-user-refresh-token")).isPresent();
    }

    @Test
    @DisplayName("미인증 시 401 반환")
    void withdraw_unauthorized_returns401() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내가 작성한 피드 조회 성공 - 200 OK")
    void getMyFeeds_success() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeed(user);
        feedImageRepository.save(FeedImage.builder()
                .feed(feed)
                .s3ObjectKey("feeds/test_image.jpg")
                .build());

        // when & then
        mockMvc.perform(get("/api/v1/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.content[0].author.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("내가 작성한 피드 조회 실패 - 비로그인 401")
    void getMyFeeds_unauthorized_returns401() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/me/feeds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 피드 커서 페이지네이션 - size 지정 + hasNext 확인")
    void getMyFeeds_pagination_hasNext() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        Feed feed3 = createFeed(user);

        // when & then - size=2 요청, 3건 있으므로 hasNext=true
        mockMvc.perform(get("/api/v1/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNumber());
    }

    @Test
    @DisplayName("내 피드 커서 페이지네이션 - cursor + size 지정으로 두 번째 페이지 조회")
    void getMyFeeds_pagination_secondPage() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed feed1 = createFeed(user);
        Feed feed2 = createFeed(user);
        Feed feed3 = createFeed(user);

        // when & then - feed3 ID를 커서로 지정, 나머지 2건만 조회
        mockMvc.perform(get("/api/v1/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("cursor", String.valueOf(feed3.getId()))
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("내 피드 feedStatus=OPEN 필터 조회")
    void getMyFeeds_filterByFeedStatus() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed openFeed = createFeed(user);
        Feed closedFeed = createFeed(user);
        closedFeed.closeVote();
        feedRepository.save(closedFeed);

        // when & then
        mockMvc.perform(get("/api/v1/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("feedStatus", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(openFeed.getId()));
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
                .imageWidth(300)
                .imageHeight(400)
                .build());
    }

    @Test
    @DisplayName("FCM 토큰 등록/갱신 성공 - DB에 fcmToken 저장")
    void updateFcmToken_success_updatesDb() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        String newToken = "fcm_" + UUID.randomUUID();

        FcmTokenRequest request = new FcmTokenRequest(newToken);

        // when & then
        mockMvc.perform(patch("/api/v1/users/fcm")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));

        // then (DB 확인)
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getFcmToken()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("FCM 토큰 등록/갱신 실패 - 토큰이 공백이면 400")
    void updateFcmToken_blank_returns400() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        FcmTokenRequest request = new FcmTokenRequest("  "); // @NotBlank 위반

        // when & then
        mockMvc.perform(patch("/api/v1/users/fcm")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FCM 토큰 등록/갱신 실패 - 미인증 401")
    void updateFcmToken_unauthorized_returns401() throws Exception {
        // given
        FcmTokenRequest request = new FcmTokenRequest("fcm_" + UUID.randomUUID());

        // when & then
        mockMvc.perform(patch("/api/v1/users/fcm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

}
