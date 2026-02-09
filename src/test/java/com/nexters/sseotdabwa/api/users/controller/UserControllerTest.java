package com.nexters.sseotdabwa.api.users.controller;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
        voteLogRepository.save(VoteLog.builder().user(otherUser).feed(feed).choice(VoteChoice.YES).build());

        Feed otherFeed = createFeed(otherUser);
        voteLogRepository.save(VoteLog.builder().user(user).feed(otherFeed).choice(VoteChoice.NO).build());

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
    @DisplayName("미인증 시 401 반환")
    void withdraw_unauthorized_returns401() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
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
}
