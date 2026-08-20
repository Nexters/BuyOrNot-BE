package com.nexters.sseotdabwa.domain.notifications.service;

import java.util.List;
import java.util.UUID;

import com.nexters.sseotdabwa.domain.notifications.entity.PushLog;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.repository.PushLogRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Transactional
class PushLogServiceTest {

    @Autowired
    private PushLogService pushLogService;

    @Autowired
    private PushLogRepository pushLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("발송 성공 로그가 success=true로 저장된다")
    void record_savesSuccessLog() {
        // given
        User user = createUser();

        // when
        pushLogService.record(user.getId(), 1L, NotificationType.MY_FEED_VOTED_1,
                "제목", "본문", true, null);

        // then
        List<PushLog> logs = pushLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).isSuccess()).isTrue();
        assertThat(logs.get(0).getErrorMessage()).isNull();
        assertThat(logs.get(0).getType()).isEqualTo(NotificationType.MY_FEED_VOTED_1);
    }

    @Test
    @DisplayName("발송 실패 로그가 success=false + 에러 메시지와 함께 저장된다")
    void record_savesFailureLog() {
        // given
        User user = createUser();

        // when
        pushLogService.record(user.getId(), 1L, NotificationType.MY_FEED_CLOSED,
                "제목", "본문", false, "FCM token invalid");

        // then
        List<PushLog> logs = pushLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).isSuccess()).isFalse();
        assertThat(logs.get(0).getErrorMessage()).isEqualTo("FCM token invalid");
    }

    @Test
    @DisplayName("feedId, type이 없는 발송(테스트 푸시)도 저장 가능하다")
    void record_savesLog_withoutFeedIdAndType() {
        // given
        User user = createUser();

        // when
        pushLogService.record(user.getId(), null, null, "테스트 제목", "테스트 본문", true, null);

        // then
        List<PushLog> logs = pushLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getFeedId()).isNull();
        assertThat(logs.get(0).getType()).isNull();
    }

    @Test
    @DisplayName("로그 저장 자체가 실패해도 예외를 전파하지 않는다 (best-effort)")
    void record_swallowsException_whenRepositorySaveFails() {
        // given
        PushLogRepository failingRepository = mock(PushLogRepository.class);
        given(failingRepository.save(any())).willThrow(new RuntimeException("DB down"));
        PushLogService service = new PushLogService(failingRepository);

        // when & then
        assertThatCode(() -> service.record(1L, 1L, NotificationType.MY_FEED_VOTED_1,
                "제목", "본문", true, null))
                .doesNotThrowAnyException();
    }

    // ---------------- helpers ----------------

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("user_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }
}
