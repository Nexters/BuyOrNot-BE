package com.nexters.sseotdabwa.api.notifications.controller;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private FeedRepository feedRepository;
    @Autowired private NotificationRepository notificationRepository;

    @MockBean
    private FcmSender fcmSender;

    @Test
    @DisplayName("알림 조회 성공 - 200 OK, data 배열 반환")
    void getNotifications_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        Feed feed = createFeed(user);
        notificationRepository.save(Notification.builder()
                .user(user)
                .feed(feed)
                .type(NotificationType.MY_FEED_CLOSED)
                .title("투표 종료!")
                .body("body")
                .build());

        // when & then
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].notificationId").exists())
                .andExpect(jsonPath("$.data[0].feedId").value(feed.getId()))
                .andExpect(jsonPath("$.data[0].title").value("투표 종료!"));
    }

    @Test
    @DisplayName("알림 조회 실패 - 인증 없으면 401")
    void getNotifications_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("알림 읽음 처리 성공 - 200 OK")
    void readNotification_success() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());
        Feed feed = createFeed(user);

        Notification noti = notificationRepository.save(Notification.builder()
                .user(user)
                .feed(feed)
                .type(NotificationType.MY_FEED_CLOSED)
                .title("투표 종료!")
                .body("body")
                .build());

        // when & then
        mockMvc.perform(patch("/api/v1/notifications/" + noti.getId() + "/read")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"));

        // DB 반영까지 확인하고 싶으면 repository로 재조회해서 assert 하는 테스트 추가 가능
    }

    @Test
    @DisplayName("알림 읽음 처리 실패 - 없는 알림이면 404")
    void readNotification_notFound_404() throws Exception {
        // given
        User user = createUser();
        String token = jwtTokenService.createAccessToken(user.getId());

        // when & then
        mockMvc.perform(patch("/api/v1/notifications/999999/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTI_001"));
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
