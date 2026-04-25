package com.nexters.sseotdabwa.api.feeds.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import com.nexters.sseotdabwa.api.feeds.dto.FeedCreateRequestV2;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.entity.UserBlock;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserBlockRepository;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

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

    @Autowired
    private VoteLogRepository voteLogRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    // ===== 피드 등록 V1 =====

    @Test
    @DisplayName("[V1] 피드 등록 성공 - 단일 이미지")
    void createFeed_v1_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.FOOD,
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                "feeds/image1.jpg",
                1080,
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedId").exists());
    }

    @Test
    @DisplayName("[V1] 피드 등록 실패 - s3ObjectKey 빈 문자열이면 400")
    void createFeed_v1_invalid_blankKey_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.FOOD,
                8000L,
                "내용",
                "",  // @NotBlank 검증 대상
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
    @DisplayName("[V1] 피드 등록 실패 - content 100자 초과면 400")
    void createFeed_v1_invalid_contentTooLong_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.FOOD,
                8000L,
                "a".repeat(101),
                "feeds/test.jpg",
                1080,
                1350
        );

        // when & then
        mockMvc.perform(post("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[V1] 피드 등록 실패 - imageWidth 0이면 400")
    void createFeed_v1_invalid_imageWidth_returns400() throws Exception {
        // given
        User user = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequest request = new FeedCreateRequest(
                FeedCategory.FOOD,
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                "feeds/uuid_test.jpg",
                0,  // @Positive 검증 대상
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

    // ===== 피드 등록 V2 =====

    @Test
    @DisplayName("[V2] 피드 등록 성공 - 이미지 1개")
    void createFeed_v2_success_singleImage() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "두쫀쿠 맛있어보이는데 살까 말까",
                List.of(new FeedCreateRequestV2.ImageRequest("feeds/image1.jpg", 1080, 1350)),
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedId").exists());
    }

    @Test
    @DisplayName("[V2] 피드 등록 성공 - 이미지 3개(최대), DB에 FeedImage 3건 저장")
    void createFeed_v2_success_maxImages() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "이미지 3개 테스트",
                List.of(
                        new FeedCreateRequestV2.ImageRequest("feeds/1.jpg", 1080, 1350),
                        new FeedCreateRequestV2.ImageRequest("feeds/2.jpg", 720, 960),
                        new FeedCreateRequestV2.ImageRequest("feeds/3.jpg", 400, 500)
                ),
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedId").exists());

        // DB에 이미지 3건 저장 확인
        org.assertj.core.api.Assertions.assertThat(feedImageRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("[V2] 피드 등록 실패 - 이미지 0개면 400 (NotEmpty)")
    void createFeed_v2_invalid_imageEmpty_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "이미지 없음",
                List.of(),  // @NotEmpty 검증 대상
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("[V2] 피드 등록 실패 - 이미지 4개 이상이면 400 (Size)")
    void createFeed_v2_invalid_imageTooMany_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "이미지 4개",
                List.of(
                        new FeedCreateRequestV2.ImageRequest("1.jpg", 100, 100),
                        new FeedCreateRequestV2.ImageRequest("2.jpg", 100, 100),
                        new FeedCreateRequestV2.ImageRequest("3.jpg", 100, 100),
                        new FeedCreateRequestV2.ImageRequest("4.jpg", 100, 100)
                ),
                null,
                null
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ===== 피드 리스트 조회 V1 =====

    @Test
    @DisplayName("[V1] 피드 리스트 조회 성공 - viewUrl(단건) 반환")
    void getFeedList_v1_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.content[0].author.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.content[0].viewUrl").value(org.hamcrest.Matchers.startsWith("https://")));
    }

    @Test
    @DisplayName("[V1] 피드 리스트 조회 - 이미지 여러 장이어도 첫 번째 이미지만 viewUrl로 반환")
    void getFeedList_v1_multipleImages_returnsFirstOnly() throws Exception {
        // given
        User user = createUser();
        Feed feed = feedRepository.save(Feed.builder()
                .user(user).content("다중이미지").price(1000L).category(FeedCategory.ELECTRONICS).build());

        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img1.jpg").imageWidth(100).imageHeight(100).build());
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img2.jpg").imageWidth(100).imageHeight(100).build());

        // when & then - V1은 첫 번째 이미지만 viewUrl(단건) 반환
        mockMvc.perform(get("/api/v1/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].viewUrl").exists())
                .andExpect(jsonPath("$.data.content[0].s3ObjectKey").value("img1.jpg"));
    }

    @Test
    @DisplayName("[V1] 피드 리스트 조회 성공 - 비로그인 유저도 접근 가능")
    void getFeedList_v1_noAuth_success() throws Exception {
        // given
        User user = createUser();
        createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ===== 피드 리스트 조회 V2 =====

    @Test
    @DisplayName("[V2] 피드 리스트 조회 성공 - 비로그인 유저도 접근 가능")
    void getFeedList_v2_noAuth_success() throws Exception {
        // given
        User user = createUser();
        createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v2/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("[V2] 피드 단건 조회 성공 - 비로그인 유저도 접근 가능")
    void getFeedDetail_v2_noAuth_success() throws Exception {
        // given
        User user = createUser();
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v2/feeds/" + feed.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.hasVoted").doesNotExist());
    }

    @Test
    @DisplayName("[V2] 피드 리스트 조회 성공 - imageUrls(다중) 반환")
    void getFeedList_v2_success() throws Exception {
        // given
        User me = createUser();
        User other = createUser();
        String token = jwtTokenService.createAccessToken(me.getId());
        Feed feed = feedRepository.save(Feed.builder()
                .user(other).content("다중이미지").price(1000L).category(FeedCategory.ELECTRONICS).build());

        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img1.jpg").imageWidth(100).imageHeight(100).build());
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img2.jpg").imageWidth(200).imageHeight(200).build());

        // when & then - 로그인 시 본인 피드 제외, 타인 피드는 조회됨
        mockMvc.perform(get("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].images").isArray())
                .andExpect(jsonPath("$.data.content[0].images.length()").value(2));
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

    // ===== 피드 신고 =====

    @Test
    @DisplayName("피드 신고 성공 - 200 OK")
    void reportFeed_success() throws Exception {
        // given
        User owner = createUser();
        User reporter = createUser();
        String reporterToken = jwtTokenService.createAccessToken(reporter.getId());
        Feed feed = createFeedWithImage(owner);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/report")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));
    }

    @Test
    @DisplayName("피드 신고 실패 - 본인 피드 신고 시 400")
    void reportFeed_selfReport_400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FEED_006"));
    }

    @Test
    @DisplayName("피드 신고 실패 - 이미 신고된 피드 재신고 시 400")
    void reportFeed_alreadyReported_400() throws Exception {
        // given
        User owner = createUser();
        User reporter = createUser();
        String reporterToken = jwtTokenService.createAccessToken(reporter.getId());
        Feed feed = createFeedWithImage(owner);
        feed.report();
        feedRepository.save(feed);

        // when & then
        mockMvc.perform(post("/api/v1/feeds/" + feed.getId() + "/report")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FEED_005"));
    }

    @Test
    @DisplayName("피드 신고 실패 - 비로그인 401")
    void reportFeed_unauthorized_401() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/feeds/1/report"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 피드 리스트 투표 상태 =====

    @Test
    @DisplayName("로그인 회원 피드 리스트 조회 - 투표한 피드에 hasVoted=true, myVoteChoice 포함")
    void getFeedList_withVote_hasVotedTrue() throws Exception {
        // given
        User owner = createUser();
        User voter = createUser();
        String voterToken = jwtTokenService.createAccessToken(voter.getId());
        Feed feed = createFeedWithImage(owner);
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hasVoted").value(true))
                .andExpect(jsonPath("$.data.content[0].myVoteChoice").value("YES"));
    }

    @Test
    @DisplayName("비로그인 게스트 피드 리스트 조회 - hasVoted, myVoteChoice null")
    void getFeedList_guest_noVoteStatus() throws Exception {
        // given
        User owner = createUser();
        createFeedWithImage(owner);

        // when & then
        mockMvc.perform(get("/api/v1/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hasVoted").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].myVoteChoice").doesNotExist());
    }

    @Test
    @DisplayName("로그인 회원 피드 리스트 조회 - 투표 안 한 피드에 hasVoted=false")
    void getFeedList_noVote_hasVotedFalse() throws Exception {
        // given
        User owner = createUser();
        User viewer = createUser();
        String viewerToken = jwtTokenService.createAccessToken(viewer.getId());
        createFeedWithImage(owner);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].hasVoted").value(false))
                .andExpect(jsonPath("$.data.content[0].myVoteChoice").doesNotExist());
    }

    // ===== 피드 리스트 커서 페이지네이션 =====

    @Test
    @DisplayName("피드 리스트 첫 페이지 조회 - size 지정, hasNext=true")
    void getFeedList_firstPage_hasNextTrue() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        createFeedWithImage(user);
        createFeedWithImage(user);
        createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").exists());
    }

    @Test
    @DisplayName("피드 리스트 두 번째 페이지 조회 - cursor+size 지정")
    void getFeedList_secondPage_withCursor() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed1 = createFeedWithImage(user);
        Feed feed2 = createFeedWithImage(user);
        Feed feed3 = createFeedWithImage(user);

        // when & then - feed3.id를 커서로, feed2와 feed1만 반환
        mockMvc.perform(get("/api/v1/feeds")
                        .param("cursor", String.valueOf(feed3.getId()))
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("피드 리스트 조회 - 파라미터 없이 요청 시 기본값 적용")
    void getFeedList_defaultParams() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("비로그인 유저 페이지네이션 조회")
    void getFeedList_noAuth_pagination() throws Exception {
        // given
        User user = createUser();
        createFeedWithImage(user);
        createFeedWithImage(user);
        createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").exists());
    }

    // ===== 피드 단건 조회 V1 =====

    @Test
    @DisplayName("[V1] 피드 단건 조회 성공 - viewUrl(단건) 반환")
    void getFeedDetail_v1_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds/" + feed.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.content").value("테스트 피드"))
                .andExpect(jsonPath("$.data.author.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.viewUrl").value(org.hamcrest.Matchers.startsWith("https://")));
    }

    @Test
    @DisplayName("[V1] 피드 단건 조회 성공 - 비로그인 유저도 접근 가능, hasVoted/myVoteChoice null")
    void getFeedDetail_v1_noAuth_success() throws Exception {
        // given
        User user = createUser();
        Feed feed = createFeedWithImage(user);

        // when & then
        mockMvc.perform(get("/api/v1/feeds/" + feed.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.hasVoted").doesNotExist())
                .andExpect(jsonPath("$.data.myVoteChoice").doesNotExist());
    }

    @Test
    @DisplayName("피드 단건 조회 실패 - 존재하지 않는 피드 404")
    void getFeedDetail_notFound_404() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(get("/api/v1/feeds/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FEED_003"));
    }

    @Test
    @DisplayName("피드 단건 조회 - 투표한 유저 hasVoted=true, myVoteChoice 포함")
    void getFeedDetail_withVote_hasVotedTrue() throws Exception {
        // given
        User owner = createUser();
        User voter = createUser();
        String voterToken = jwtTokenService.createAccessToken(voter.getId());
        Feed feed = createFeedWithImage(owner);
        voteLogRepository.save(VoteLog.builder().user(voter).feed(feed).choice(VoteChoice.YES).voteType(VoteType.USER).build());

        // when & then
        mockMvc.perform(get("/api/v1/feeds/" + feed.getId())
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasVoted").value(true))
                .andExpect(jsonPath("$.data.myVoteChoice").value("YES"));
    }

    // ===== 피드 단건 조회 V2 =====

    @Test
    @DisplayName("[V2] 피드 단건 조회 성공 - imageUrls(다중) 반환")
    void getFeedDetail_v2_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = feedRepository.save(Feed.builder()
                .user(user).content("다중이미지 피드").price(10000L).category(FeedCategory.FASHION).build());

        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img1.jpg").imageWidth(300).imageHeight(400).build());
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img2.jpg").imageWidth(200).imageHeight(300).build());

        // when & then
        mockMvc.perform(get("/api/v2/feeds/" + feed.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data.images").isArray())
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andExpect(jsonPath("$.data.images[0].imageWidth").value(300))
                .andExpect(jsonPath("$.data.images[0].imageHeight").value(400));
    }

    // ===== 피드 리스트 feedStatus 필터 =====

    @Test
    @DisplayName("피드 리스트 조회 - feedStatus=OPEN 필터 적용")
    void getFeedList_filterByOpenStatus() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed openFeed = createFeedWithImage(user);
        Feed closedFeed = createFeedWithImage(user);
        closedFeed.closeVote();
        feedRepository.save(closedFeed);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .param("feedStatus", "OPEN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(openFeed.getId()));
    }

    @Test
    @DisplayName("피드 리스트 조회 - feedStatus=CLOSED 필터 적용")
    void getFeedList_filterByClosedStatus() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        createFeedWithImage(user);
        Feed closedFeed = createFeedWithImage(user);
        closedFeed.closeVote();
        feedRepository.save(closedFeed);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .param("feedStatus", "CLOSED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(closedFeed.getId()));
    }

    @Test
    @DisplayName("피드 리스트 조회 - feedStatus 미지정 시 전체 반환")
    void getFeedList_noStatusFilter_returnsAll() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        createFeedWithImage(user);
        Feed closedFeed = createFeedWithImage(user);
        closedFeed.closeVote();
        feedRepository.save(closedFeed);

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    // ===== 차단 사용자 피드 필터링 =====

    @Test
    @DisplayName("로그인 사용자 피드 리스트 조회 - 차단한 사용자의 피드 미표시")
    void getFeedList_blockedUserFeedNotShown() throws Exception {
        // given
        User viewer = createUser();
        User blocked = createUser();
        String viewerToken = jwtTokenService.createAccessToken(viewer.getId());

        createFeedWithImage(viewer);
        Feed blockedFeed = createFeedWithImage(blocked);
        userBlockRepository.save(UserBlock.builder().user(viewer).blockedUser(blocked).build());

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(blockedFeed.getId().intValue()))));
    }

    @Test
    @DisplayName("비로그인 사용자 피드 리스트 조회 - 전체 피드 표시")
    void getFeedList_guestSeesAllFeeds() throws Exception {
        // given
        User user1 = createUser();
        User user2 = createUser();
        Feed feed1 = createFeedWithImage(user1);
        Feed feed2 = createFeedWithImage(user2);

        // when & then
        mockMvc.perform(get("/api/v1/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItems(feed1.getId().intValue(), feed2.getId().intValue())));
    }

    @Test
    @DisplayName("차단 사용자 피드 제외 후 페이지네이션 정확성 확인")
    void getFeedList_blockFilterWithPagination() throws Exception {
        // given
        User viewer = createUser();
        User blocked = createUser();
        String viewerToken = jwtTokenService.createAccessToken(viewer.getId());

        Feed myFeed1 = createFeedWithImage(viewer);
        Feed blockedFeed = createFeedWithImage(blocked);
        Feed myFeed2 = createFeedWithImage(viewer);

        userBlockRepository.save(UserBlock.builder().user(viewer).blockedUser(blocked).build());

        // when & then - size=2, 차단 피드 제외 후 내 피드 2개만 반환
        mockMvc.perform(get("/api/v1/feeds")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(blockedFeed.getId().intValue()))));
    }

    // ===== 피드 등록 V2 link/title =====

    @Test
    @DisplayName("[V2] 피드 등록 성공 - 유효한 link와 title 포함")
    void createFeed_v2_withLinkAndTitle_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "링크 포함 피드",
                List.of(new FeedCreateRequestV2.ImageRequest("feeds/image1.jpg", 1080, 1350)),
                "https://www.example.com/product/123",
                "이 신발 살까 말까"
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feedId").exists());
    }

    @Test
    @DisplayName("[V2] 피드 등록 실패 - http/https 아닌 link면 400")
    void createFeed_v2_invalidLink_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "잘못된 링크",
                List.of(new FeedCreateRequestV2.ImageRequest("feeds/image1.jpg", 1080, 1350)),
                "ftp://invalid.com",
                null
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("[V2] 피드 등록 실패 - 호스트 없는 URL이면 400")
    void createFeed_v2_linkWithoutHost_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "호스트 없는 링크",
                List.of(new FeedCreateRequestV2.ImageRequest("feeds/image1.jpg", 1080, 1350)),
                "https://foo",  // 점 없는 호스트
                null
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("[V2] 피드 등록 실패 - title 40자 초과면 400")
    void createFeed_v2_titleTooLong_returns400() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        FeedCreateRequestV2 request = new FeedCreateRequestV2(
                FeedCategory.FOOD,
                8000L,
                "내용",
                List.of(new FeedCreateRequestV2.ImageRequest("feeds/image1.jpg", 1080, 1350)),
                null,
                "a".repeat(41)  // @Size(max=40)
        );

        // when & then
        mockMvc.perform(post("/api/v2/feeds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }

    @Test
    @DisplayName("[V2] 피드 단건 조회 - link와 title 반환 확인")
    void getFeedDetail_v2_returnsLinkAndTitle() throws Exception {
        // given
        User user = createUser();
        Feed feed = feedRepository.save(Feed.builder()
                .user(user).content("링크 피드").price(5000L).category(FeedCategory.FOOD)
                .link("https://www.example.com")
                .title("살까 말까")
                .build());
        feedImageRepository.save(FeedImage.builder().feed(feed).s3ObjectKey("img1.jpg").imageWidth(100).imageHeight(100).build());

        // when & then
        mockMvc.perform(get("/api/v2/feeds/" + feed.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.link").value("https://www.example.com"))
                .andExpect(jsonPath("$.data.title").value("살까 말까"));
    }

    // ===== 피드 리스트 V2 본인 피드 제외 =====

    @Test
    @DisplayName("[V2] 피드 리스트 조회 - 로그인 시 본인 피드 자동 제외")
    void getFeedList_v2_loggedIn_excludesMyFeeds() throws Exception {
        // given
        User me = createUser();
        User other = createUser();
        String myToken = jwtTokenService.createAccessToken(me.getId());

        Feed myFeed = createFeedWithImage(me);
        Feed otherFeed = createFeedWithImage(other);

        // when & then
        mockMvc.perform(get("/api/v2/feeds")
                        .header("Authorization", "Bearer " + myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(myFeed.getId().intValue()))))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItem(otherFeed.getId().intValue())));
    }

    @Test
    @DisplayName("[V2] 피드 리스트 조회 - 비로그인 시 전체 피드 표시")
    void getFeedList_v2_noAuth_showsAllFeeds() throws Exception {
        // given
        User user1 = createUser();
        User user2 = createUser();
        Feed feed1 = createFeedWithImage(user1);
        Feed feed2 = createFeedWithImage(user2);

        // when & then
        mockMvc.perform(get("/api/v2/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItems(feed1.getId().intValue(), feed2.getId().intValue())));
    }

    @Test
    @DisplayName("피드 리스트 조회 - category 필터 적용")
    void getFeedList_filterByCategory() throws Exception {
        // given
        User user = createUser();
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .param("category", "FASHION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(fashionFeed.getId()));
    }

    @Test
    @DisplayName("[V2] 피드 리스트 조회 - category 필터 적용")
    void getFeedList_v2_filterByCategory() throws Exception {
        // given
        User user = createUser();
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v2/feeds")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].feedId").value(foodFeed.getId()));
    }

    @Test
    @DisplayName("피드 리스트 조회 - category 복수 선택 필터 적용")
    void getFeedList_filterByMultipleCategories() throws Exception {
        // given
        User user = createUser();
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        Feed bookFeed = feedRepository.save(Feed.builder().user(user).content("책").price(1000L).category(FeedCategory.BOOK).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f1.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f2.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(bookFeed).s3ObjectKey("f3.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v1/feeds")
                        .param("category", "FASHION")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItems(fashionFeed.getId().intValue(), foodFeed.getId().intValue())));
    }

    @Test
    @DisplayName("[V2] 피드 리스트 조회 - category 복수 선택 필터 적용")
    void getFeedList_v2_filterByMultipleCategories() throws Exception {
        // given
        User user = createUser();
        Feed fashionFeed = feedRepository.save(Feed.builder().user(user).content("패션").price(1000L).category(FeedCategory.FASHION).build());
        Feed foodFeed = feedRepository.save(Feed.builder().user(user).content("음식").price(1000L).category(FeedCategory.FOOD).build());
        Feed bookFeed = feedRepository.save(Feed.builder().user(user).content("책").price(1000L).category(FeedCategory.BOOK).build());
        feedImageRepository.save(FeedImage.builder().feed(fashionFeed).s3ObjectKey("f1.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(foodFeed).s3ObjectKey("f2.jpg").imageWidth(10).imageHeight(10).build());
        feedImageRepository.save(FeedImage.builder().feed(bookFeed).s3ObjectKey("f3.jpg").imageWidth(10).imageHeight(10).build());

        // when & then
        mockMvc.perform(get("/api/v2/feeds")
                        .param("category", "FASHION")
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItems(fashionFeed.getId().intValue(), foodFeed.getId().intValue())));
    }

    // ===== 신고된 피드 필터링 =====

    @Test
    @DisplayName("피드 리스트 조회 - 신고된(REPORTED) 피드 미표시")
    void getFeedList_reportedFeedNotShown() throws Exception {
        // given
        User user = createUser();
        Feed normalFeed = createFeedWithImage(user);
        Feed reportedFeed = createFeedWithImage(user);
        reportedFeed.report();
        feedRepository.save(reportedFeed);

        // when & then
        mockMvc.perform(get("/api/v1/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItem(normalFeed.getId().intValue())))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(reportedFeed.getId().intValue()))));
    }

    @Test
    @DisplayName("[V2] 피드 리스트 조회 - 신고된(REPORTED) 피드 미표시")
    void getFeedList_v2_reportedFeedNotShown() throws Exception {
        // given
        User user = createUser();
        Feed normalFeed = createFeedWithImage(user);
        Feed reportedFeed = createFeedWithImage(user);
        reportedFeed.report();
        feedRepository.save(reportedFeed);

        // when & then
        mockMvc.perform(get("/api/v2/feeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.hasItem(normalFeed.getId().intValue())))
                .andExpect(jsonPath("$.data.content[*].feedId",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(reportedFeed.getId().intValue()))));
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
                .build());

        feedImageRepository.save(FeedImage.builder()
                .feed(feed)
                .s3ObjectKey("feeds/test_" + UUID.randomUUID() + ".jpg")
                .imageWidth(1080)
                .imageHeight(1350)
                .build());

        return feed;
    }
}
