package com.nexters.sseotdabwa.api.users.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.nexters.sseotdabwa.api.users.dto.FcmTokenRequest;

import com.nexters.sseotdabwa.domain.users.entity.UserBlock;
import com.nexters.sseotdabwa.domain.users.repository.UserBlockRepository;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private UserBlockRepository userBlockRepository;

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
    @DisplayName("내 정보 조회 성공 - 200 OK")
    void getMyInfo_success() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.data.socialAccount").value(user.getSocialAccount().name()));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 비로그인 401")
    void getMyInfo_unauthorized_returns401() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

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
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("key1").imageWidth(100).imageHeight(100).build());
        feedReviewRepository.save(FeedReview.builder().feed(feed).content("리뷰").build());
        voteLogRepository.save(VoteLog.builder().user(otherUser).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());

        Feed otherFeed = createFeed(otherUser);
        voteLogRepository.save(VoteLog.builder().user(user).feed(otherFeed).choice(VoteChoice.NO).voteType(VoteType.USER).build());

        userBlockRepository.save(UserBlock.builder().user(user).blockedUser(otherUser).build());
        userBlockRepository.save(UserBlock.builder().user(otherUser).blockedUser(user).build());

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
        assertThat(userBlockRepository.count()).isZero();
    }

    @Test
    @DisplayName("회원 탈퇴 시 다른 유저의 데이터는 영향 없음")
    void withdraw_success_doesNotAffectOtherUsers() throws Exception {
        // given
        User user = createUser();
        User otherUser = createUser();
        Feed otherFeed = createFeed(otherUser);
        feedImageRepository.save(FeedImage.builder().feed(otherFeed).s3ObjectKey("other-key").imageWidth(100).imageHeight(100).build());
        userBlockRepository.save(UserBlock.builder().user(otherUser).blockedUser(createUser()).build());

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // then
        assertThat(userRepository.findById(otherUser.getId())).isPresent();
        assertThat(feedRepository.findByUserId(otherUser.getId())).hasSize(1);
        assertThat(feedImageRepository.count()).isEqualTo(1);
        assertThat(userBlockRepository.count()).isEqualTo(1);
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
                .imageWidth(1080)
                .imageHeight(1350)
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

    @Test
    @DisplayName("내가 작성한 피드 조회 - category 필터 적용")
    void getMyFeeds_filterByCategory() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v1/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("category", "FASHION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(fashionFeed.getId()));
    }

    @Test
    @DisplayName("[V2] 내가 작성한 피드 조회 - category 필터 적용")
    void getMyFeeds_v2_filterByCategory() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v2/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(foodFeed.getId()));
    }

    @Test
    @DisplayName("내가 작성한 피드 조회 - category 복수 선택 필터 적용")
    void getMyFeeds_filterByMultipleCategories() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        Feed bookFeed = feedRepository.save(Feed.builder().user(user).content("책").price(1000L).category(FeedCategory.BOOK).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f1.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f2.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(bookFeed).s3ObjectKey("f3.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v1/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("category", "FASHION")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItems(fashionFeed.getId().intValue(), foodFeed.getId().intValue())));
    }

    @Test
    @DisplayName("[V2] 내가 작성한 피드 조회 - category 복수 선택 필터 적용")
    void getMyFeeds_v2_filterByMultipleCategories() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        Feed bookFeed = feedRepository.save(Feed.builder().user(user).content("책").price(1000L).category(FeedCategory.BOOK).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f1.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f2.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(bookFeed).s3ObjectKey("f3.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v2/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("category", "FASHION")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItems(fashionFeed.getId().intValue(), foodFeed.getId().intValue())));
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

    // ===== 내가 작성한 피드 조회 V2 =====

    @Test
    @DisplayName("[V2] 내가 작성한 피드 조회 성공 - imageUrls(다중) 반환")
    void getMyFeeds_v2_success_multipleImages() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeed(user);
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("feeds/img1.jpg").imageWidth(100).imageHeight(100).build());
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("feeds/img2.jpg").imageWidth(100).imageHeight(100).build());

        // when & then
        mockMvc.perform(get("/api/v2/users/me/feeds")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.content[0].images").isArray())
                .andExpect(jsonPath("$.data.content[0].images.length()").value(2));
    }

    @Test
    @DisplayName("[V2] 내가 작성한 피드 조회 실패 - 비로그인 401")
    void getMyFeeds_v2_unauthorized_returns401() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v2/users/me/feeds"))
                .andExpect(status().isUnauthorized());
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

    @Test
    @DisplayName("사용자 차단 성공 - 201 CREATED")
    void blockUser_success() throws Exception {
        // given
        User user = createUser();
        User target = createUser();

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(post("/api/v1/users/blocks/{userId}", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"));
    }

    @Test
    @DisplayName("사용자 차단 실패 - 자기 자신 차단 400")
    void blockUser_selfBlock_returns400() throws Exception {
        // given
        User user = createUser();
        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(post("/api/v1/users/blocks/{userId}", user.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("차단 사용자 목록 조회 성공")
    void getBlockedUsers_success() throws Exception {
        // given
        User user = createUser();
        User target = createUser();

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // 먼저 차단
        mockMvc.perform(post("/api/v1/users/blocks/{userId}", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(get("/api/v1/users/blocks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].userId").value(target.getId()));
    }

    @Test
    @DisplayName("사용자 차단 해제 성공")
    void unblockUser_success() throws Exception {
        // given
        User user = createUser();
        User target = createUser();

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // 차단
        mockMvc.perform(post("/api/v1/users/blocks/{userId}", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated());

        // when
        mockMvc.perform(delete("/api/v1/users/blocks/{userId}", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("차단 해제 실패 - 차단 관계 없음 404")
    void unblockUser_notFound_returns404() throws Exception {
        // given
        User user = createUser();
        User target = createUser();

        String accessToken = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(delete("/api/v1/users/blocks/{userId}", target.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("차단 API 실패 - 인증 없음 401")
    void blockUser_unauthorized_returns401() throws Exception {

        User target = createUser();

        mockMvc.perform(post("/api/v1/users/blocks/{userId}", target.getId()))
                .andExpect(status().isUnauthorized());
    }
}
