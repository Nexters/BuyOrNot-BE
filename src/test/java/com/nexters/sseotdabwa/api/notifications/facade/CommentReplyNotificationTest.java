package com.nexters.sseotdabwa.api.notifications.facade;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.facade.CommentFacade;
import com.nexters.sseotdabwa.api.notifications.dto.NotificationResponse;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.comments.service.CommentRateLimiter;
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.notifications.email.EmailMessage;
import com.nexters.sseotdabwa.domain.notifications.email.EmailSender;
import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.entity.PushLog;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationChannel;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.repository.NotificationRepository;
import com.nexters.sseotdabwa.domain.notifications.repository.PushLogRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.entity.UserBlock;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserBlockRepository;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 답글 알림 (수신자 산정 / 문구 / 푸시·이메일 채널 / 30분 메일 제한 / 수신 거부)
 * - onCommentReplied는 트랜잭션이 있으면 참여하므로, 테스트 트랜잭션 안에서 바로 호출해 검증한다
 */
@SpringBootTest
@Transactional
class CommentReplyNotificationTest {

    @Autowired private NotificationFacade notificationFacade;
    @Autowired private CommentFacade commentFacade;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserBlockRepository userBlockRepository;
    @Autowired private FeedRepository feedRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PushLogRepository pushLogRepository;

    @MockBean private FcmSender fcmSender;
    @MockBean private EmailSender emailSender;
    @MockBean private CommentRateLimiter commentRateLimiter; // 빈도 제한은 CommentFacadeTest에서 검증 (컨텍스트 간 userId 재사용으로 인한 오탐 방지)

    @Test
    @DisplayName("원댓글 작성자 + 스레드 답글 작성자에게 알림, 답글 작성자 본인과 게스트는 제외")
    void recipients_rootAuthorAndParticipants_excludingReplier() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        User participant = createUser("participant");
        User replier = createUser("replier");
        Long rootId = comment(rootAuthor, feed);
        reply(participant, feed, rootId);
        reply(replier, feed, rootId); // 본인이 예전에 단 답글이 있어도 본인은 제외되어야 함

        Long replyId = reply(replier, feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        assertThat(replyNotificationReceivers(replyId))
                .containsExactlyInAnyOrder(rootAuthor.getId(), participant.getId());
    }

    @Test
    @DisplayName("게스트가 답글을 달아도 원댓글 작성자와 스레드 참여 회원에게 알림이 간다 (게스트 닉네임으로)")
    void guestReply_notifiesMembers() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        User participant = createUser("participant");
        Long rootId = comment(rootAuthor, feed);
        reply(participant, feed, rootId);

        Long replyId = commentFacade.createGuestReply(
                feed.getId(), rootId,
                new CommentCreateRequestGuest("게스트 답글", "지름신들린수달_1234", "guest-password"),
                "ip-" + UUID.randomUUID(), null
        ).id();
        notificationFacade.onCommentReplied(replyId);

        assertThat(replyNotificationReceivers(replyId)).containsExactlyInAnyOrder(rootAuthor.getId(), participant.getId());
        String guestNickname = commentRepository.findById(replyId).orElseThrow().getDisplayNickname();
        assertThat(replyNotificationOf(rootAuthor, replyId).getTitle())
                .isEqualTo("'" + guestNickname + "'님이 내 의견에 답글을 남겼어요");
    }

    @Test
    @DisplayName("답글 작성자를 차단한 회원에게는 알림을 보내지 않는다")
    void recipients_excludeUsersWhoBlockedReplier() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        User replier = createUser("replier");
        Long rootId = comment(rootAuthor, feed);
        userBlockRepository.save(UserBlock.builder().user(rootAuthor).blockedUser(replier).build());

        Long replyId = reply(replier, feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        assertThat(replyNotificationReceivers(replyId)).isEmpty();
    }

    @Test
    @DisplayName("같은 피드에 답글이 여러 개 달리면 답글마다 알림이 저장된다 (기존 타입의 중복 방지와 별개)")
    void notificationPerReply() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        Long rootId = comment(rootAuthor, feed);

        Long reply1 = reply(createUser("a"), feed, rootId);
        Long reply2 = reply(createUser("b"), feed, rootId);
        notificationFacade.onCommentReplied(reply1);
        notificationFacade.onCommentReplied(reply2);
        notificationFacade.onCommentReplied(reply2); // 같은 답글 재처리는 중복 저장 안 됨

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(rootAuthor.getId()) && n.getType() == NotificationType.COMMENT_REPLIED)
                .count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("문구: 원댓글 작성자와 참여자에게 다른 title, body는 답글 내용 30자 + ...")
    void titleAndBody() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        User participant = createUser("participant");
        User replier = createUser("replier");
        Long rootId = comment(rootAuthor, feed);
        reply(participant, feed, rootId);
        String longContent = "가".repeat(40);

        Long replyId = commentFacade.createReply(replier, feed.getId(), rootId, new CommentCreateRequest(longContent)).id();
        notificationFacade.onCommentReplied(replyId);

        Notification toRoot = replyNotificationOf(rootAuthor, replyId);
        Notification toParticipant = replyNotificationOf(participant, replyId);
        assertThat(toRoot.getTitle()).isEqualTo("'" + replier.getNickname() + "'님이 내 의견에 답글을 남겼어요");
        assertThat(toParticipant.getTitle()).isEqualTo("'" + replier.getNickname() + "'님이 나도 참여한 대화에 답글을 남겼어요");
        assertThat(toRoot.getBody()).isEqualTo("가".repeat(30) + "...");
    }

    @Test
    @DisplayName("닉네임이 길어도 title은 60자를 넘지 않는다")
    void title_truncatedWithin60() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        User replier = userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("긴닉네임" + "가".repeat(36))
                .socialAccount(SocialAccount.KAKAO)
                .build());
        Long rootId = comment(rootAuthor, feed);

        Long replyId = reply(replier, feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        assertThat(replyNotificationOf(rootAuthor, replyId).getTitle()).hasSizeLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("앱 회원(푸시 가능)은 푸시만, 이메일은 보내지 않는다")
    void channel_push() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root", "root@example.com");
        rootAuthor.updateFcmToken("token-root");
        userRepository.save(rootAuthor);
        Long rootId = comment(rootAuthor, feed);

        Long replyId = reply(createUser("replier"), feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        verify(fcmSender, times(1)).send(eq("token-root"), anyString(), anyString(), anyMap());
        verify(emailSender, never()).send(any());
        assertThat(pushLogsOf(rootAuthor)).extracting(PushLog::getChannel).containsExactly(NotificationChannel.PUSH);
    }

    @Test
    @DisplayName("웹 회원(푸시 불가 + 이메일 있음)은 이메일 발송, 링크와 수신 거부 URL이 포함된다")
    void channel_email() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root", "root@example.com");
        Long rootId = comment(rootAuthor, feed);

        Long replyId = reply(createUser("replier"), feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, times(1)).send(captor.capture());
        EmailMessage message = captor.getValue();
        assertThat(message.to()).isEqualTo("root@example.com");
        assertThat(message.html()).contains("https://web.test/feed/" + feed.getId());
        assertThat(message.unsubscribeUrl()).startsWith("https://api.test/api/v1/notifications/email/unsubscribe?token=");
        verify(fcmSender, never()).send(anyString(), anyString(), anyString(), anyMap());

        PushLog log = pushLogsOf(rootAuthor).get(0);
        assertThat(log.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(log.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("메일 본문의 사용자 입력(답글 내용)은 HTML escape된다")
    void email_escapesUserInput() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root", "root@example.com");
        Long rootId = comment(rootAuthor, feed);

        Long replyId = commentFacade.createReply(createUser("replier"), feed.getId(), rootId,
                new CommentCreateRequest("<script>alert('x')</script>")).id();
        notificationFacade.onCommentReplied(replyId);

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(captor.capture());
        assertThat(captor.getValue().html())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("같은 스레드의 연속 답글은 이메일을 30분에 1통만 보내지만, 알림은 답글마다 저장된다")
    void email_throttledPerThread() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root", "root@example.com");
        Long rootId = comment(rootAuthor, feed);

        Long reply1 = reply(createUser("a"), feed, rootId);
        Long reply2 = reply(createUser("b"), feed, rootId);
        notificationFacade.onCommentReplied(reply1);
        notificationFacade.onCommentReplied(reply2);

        verify(emailSender, times(1)).send(any());
        assertThat(replyNotificationOf(rootAuthor, reply1)).isNotNull();
        assertThat(replyNotificationOf(rootAuthor, reply2)).isNotNull();
    }

    @Test
    @DisplayName("이메일 알림을 끈 회원 / 이메일 없는 회원에게는 메일을 보내지 않는다 (알림함은 저장)")
    void email_notSentWhenDisabledOrNoEmail() {
        Feed feed = createFeed(createUser("owner"));
        User disabled = createUser("disabled", "disabled@example.com");
        disabled.disableEmailNotification();
        userRepository.save(disabled);
        User noEmail = createUser("noemail");
        Long rootId = comment(disabled, feed);
        reply(noEmail, feed, rootId);

        Long replyId = reply(createUser("replier"), feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        verify(emailSender, never()).send(any());
        assertThat(replyNotificationReceivers(replyId)).containsExactlyInAnyOrder(disabled.getId(), noEmail.getId());
    }

    @Test
    @DisplayName("메일 발송이 실패해도 알림은 저장되고 실패 로그(EMAIL)가 남으며, 다음 수신자 처리도 계속된다")
    void email_failure_isBestEffort() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root", "root@example.com");
        User participant = createUser("participant", "p@example.com");
        Long rootId = comment(rootAuthor, feed);
        reply(participant, feed, rootId);
        doThrow(new IllegalStateException("smtp down")).when(emailSender).send(any());

        Long replyId = reply(createUser("replier"), feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        verify(emailSender, times(2)).send(any());
        assertThat(replyNotificationReceivers(replyId)).containsExactlyInAnyOrder(rootAuthor.getId(), participant.getId());
        PushLog log = pushLogsOf(rootAuthor).get(0);
        assertThat(log.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(log.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("알림 처리 전에 답글이 삭제되면 아무 것도 하지 않는다")
    void deletedReply_noop() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        User replier = createUser("replier");
        Long rootId = comment(rootAuthor, feed);
        Long replyId = reply(replier, feed, rootId);
        commentFacade.deleteComment(replier, replyId);

        notificationFacade.onCommentReplied(replyId);

        assertThat(replyNotificationReceivers(replyId)).isEmpty();
    }

    @Test
    @DisplayName("알림 목록의 답글 알림에는 답글 페이지로 이동할 원댓글 id(commentId)가 담긴다")
    void recentNotifications_containsRootCommentId() {
        Feed feed = createFeed(createUser("owner"));
        User rootAuthor = createUser("root");
        Long rootId = comment(rootAuthor, feed);

        Long replyId = reply(createUser("replier"), feed, rootId);
        notificationFacade.onCommentReplied(replyId);

        List<NotificationResponse> list = notificationFacade.getRecentNotifications(rootAuthor, NotificationType.COMMENT_REPLIED);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).commentId()).isEqualTo(rootId);
        assertThat(list.get(0).feedId()).isEqualTo(feed.getId());
    }

    // ===== 이메일 수신 거부 =====

    @Test
    @DisplayName("유효한 수신 거부 토큰이면 이메일 알림이 꺼진다 (멱등)")
    void unsubscribe_validToken() {
        User user = createUser("user", "user@example.com");
        String token = jwtTokenService.createEmailUnsubscribeToken(user.getId());

        assertThat(notificationFacade.unsubscribeEmail(token)).isTrue();
        assertThat(notificationFacade.unsubscribeEmail(token)).isTrue();
        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("위조/다른 타입(access) 토큰이면 수신 거부되지 않는다")
    void unsubscribe_invalidToken() {
        User user = createUser("user", "user@example.com");

        assertThat(notificationFacade.unsubscribeEmail("not-a-jwt")).isFalse();
        assertThat(notificationFacade.unsubscribeEmail(jwtTokenService.createAccessToken(user.getId()))).isFalse();
        assertThat(notificationFacade.unsubscribeEmail(null)).isFalse();
        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("수신 거부 토큰으로는 API 인증이 불가하다")
    void unsubscribeToken_cannotAuthenticate() {
        String token = jwtTokenService.createEmailUnsubscribeToken(1L);

        assertThat(jwtTokenService.validateToken(token)).isFalse();
        assertThat(jwtTokenService.validateRefreshToken(token)).isFalse();
    }

    // ===== Helper Methods =====

    private List<Long> replyNotificationReceivers(Long replyId) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.COMMENT_REPLIED && n.getTargetId().equals(replyId))
                .map(n -> n.getUser().getId())
                .toList();
    }

    private Notification replyNotificationOf(User user, Long replyId) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.COMMENT_REPLIED
                        && n.getTargetId().equals(replyId)
                        && n.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    private List<PushLog> pushLogsOf(User user) {
        return pushLogRepository.findAll().stream()
                .filter(l -> l.getUserId().equals(user.getId()))
                .toList();
    }

    private Long comment(User user, Feed feed) {
        return commentFacade.createComment(user, feed.getId(), new CommentCreateRequest("원댓글")).id();
    }

    private Long reply(User user, Feed feed, Long rootId) {
        return commentFacade.createReply(user, feed.getId(), rootId, new CommentCreateRequest("답글입니다")).id();
    }

    private User createUser(String prefix) {
        return createUser(prefix, null);
    }

    private User createUser(String prefix, String email) {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(prefix + "_" + UUID.randomUUID().toString().substring(0, 6))
                .socialAccount(SocialAccount.KAKAO)
                .email(email)
                .build());
    }

    private Feed createFeed(User user) {
        return feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .title("장화 살지말지")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build());
    }
}
