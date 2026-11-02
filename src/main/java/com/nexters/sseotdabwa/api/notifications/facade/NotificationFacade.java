package com.nexters.sseotdabwa.api.notifications.facade;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.api.notifications.exception.NotificationErrorCode;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.nexters.sseotdabwa.api.notifications.dto.NotificationResponse;
import com.nexters.sseotdabwa.common.config.AsyncConfig;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.config.EmailNotificationProperties;
import com.nexters.sseotdabwa.domain.auth.service.JwtTokenService;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.event.CommentRepliedEvent;
import com.nexters.sseotdabwa.domain.comments.service.CommentService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.email.CommentReplyEmailTemplate;
import com.nexters.sseotdabwa.domain.notifications.email.EmailMessage;
import com.nexters.sseotdabwa.domain.notifications.email.EmailSender;
import com.nexters.sseotdabwa.domain.notifications.email.EmailThrottle;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationChannel;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.service.NotificationService;
import com.nexters.sseotdabwa.domain.notifications.service.PushLogService;
import com.nexters.sseotdabwa.domain.notifications.service.command.NotificationResultCommand;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.service.UserBlockService;
import com.nexters.sseotdabwa.domain.users.service.UserService;
import com.nexters.sseotdabwa.domain.votes.event.VoteCreatedEvent;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 알림 관련 흐름을 조합하는 Facade
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private static final String FEED_CLOSED_BODY = "토봉이가 결과를 들고 기다리고 있어요.";
    private static final String VOTED_1_BODY = "첫 투표가 도착했어요.";
    private static final String VOTED_10_BODY = "벌써 10명이나 투표에 참여했어요!";
    private static final int FEED_TITLE_MAX_LENGTH = 10;

    // 답글 알림 문구 — 기획 확정 전 임시안. 확정되면 상수만 교체
    private static final String REPLY_TITLE_TO_ROOT_AUTHOR = "님이 내 의견에 답글을 남겼어요";
    private static final String REPLY_TITLE_TO_PARTICIPANT = "님이 나도 참여한 대화에 답글을 남겼어요";
    private static final int NOTIFICATION_TITLE_MAX_LENGTH = 60; // notifications.title 컬럼 길이
    private static final int REPLY_BODY_MAX_LENGTH = 30;
    private static final String EMAIL_UNSUBSCRIBE_PATH = "/api/v1/notifications/email/unsubscribe?token=";

    private final NotificationService notificationService;
    private final PushLogService pushLogService;
    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final VoteLogService voteLogService;
    private final UserService userService;
    private final UserBlockService userBlockService;
    private final CommentService commentService;
    private final JwtTokenService jwtTokenService;

    private final FcmSender fcmSender;
    private final EmailSender emailSender;
    private final EmailThrottle emailThrottle;
    private final AwsProperties awsProperties;
    private final EmailNotificationProperties emailProperties;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationService.countUnread(user.getId());
    }

    /**
     * 최근 30일 알림 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(User user, NotificationType type) {
        List<Notification> notifications = notificationService.getRecentNotifications(user.getId(), type);
        if (notifications.isEmpty()) {
            return List.of();
        }

        // 1) feedId 목록 추출
        List<Long> feedIds = notifications.stream()
                .map(n -> n.getFeed().getId())
                .distinct()
                .toList();

        // 2) Feed 벌크 조회 (N+1 방지)
        List<Feed> feeds = feedService.findByIds(feedIds);
        Map<Long, Feed> feedMap = feeds.stream()
                .collect(Collectors.toMap(Feed::getId, f -> f));

        // 3) FeedImage 벌크 조회 (N+1 방지)
        List<FeedImage> feedImages = feedImageService.findByFeedIds(feedIds);
        Map<Long, FeedImage> feedImageMap = feedImages.stream()
                .collect(Collectors.toMap(fi -> fi.getFeed().getId(), fi -> fi, (first, second) -> first));

        // 4) 답글 알림의 원댓글 id (targetId = 답글 id → 부모 id). 답글이 이미 삭제됐으면 null
        Map<Long, Long> rootCommentIdByReplyId = findRootCommentIds(notifications);

        // 5) 알림 -> 응답 매핑
        return notifications.stream()
                .map(n -> {
                    Long feedId = n.getFeed().getId();

                    Feed feed = feedMap.get(feedId);
                    if (feed == null) {
                        // 알림은 있는데 피드가 없다 = 데이터 정합성 깨짐 → 에러 처리
                        throw new GlobalException(NotificationErrorCode.NOTIFICATION_FEED_NOT_FOUND);
                    }

                    // 투표 종료 시간 = createdAt + 48h
                    LocalDateTime voteClosedAt = feed.getVoteClosedAt();

                    // 결과 퍼센트/라벨
                    NotificationResultCommand result = calculateResult(feed);

                    // 이미지 viewUrl (CloudFront domain + s3ObjectKey)
                    FeedImage feedImage = feedImageMap.get(feedId);
                    String viewUrl = buildCloudFrontUrl(
                            awsProperties.cloudfront().domain(),
                            feedImage == null ? null : feedImage.getS3ObjectKey()
                    );

                    return NotificationResponse.of(
                            n,
                            voteClosedAt,
                            result.resultPercent(),
                            result.resultLabel(),
                            viewUrl,
                            feed.getTitle(),
                            n.getType() == NotificationType.COMMENT_REPLIED ? rootCommentIdByReplyId.get(n.getTargetId()) : null
                    );
                })
                .toList();
    }

    /**
     * 읽음 처리 (본인 알림만)
     */
    @Transactional
    public void markAsRead(User user, Long notificationId) {
        notificationService.markAsRead(user.getId(), notificationId);
    }

    /**
     * 스케줄러에서 호출: 마감된 feedId 기반 알림 생성 + (조건부) 푸시
     *
     * 흐름:
     * - feedId들 조회(벌크)
     * - 작성자/참여자 계산
     * - Notification 저장(중복 방지)
     * - Push
     */
    @Transactional
    public void onFeedsClosed(List<Long> closedFeedIds) {
        if (closedFeedIds == null || closedFeedIds.isEmpty()) {
            return;
        }

        // 1) feed 벌크 조회 (N+1 방지)
        List<Feed> feeds = feedService.findByIds(closedFeedIds);

        for (Feed feed : feeds) {
            try {
                notifyFeedClosed(feed);
            } catch (Exception e) {
                log.error("피드 알림 생성 실패 feedId={}", feed.getId(), e);
            }
        }
    }

    /**
     * vote() 커밋 후 이벤트로 호출 — feed X-Lock 해제 후 실행되므로 FK 락 충돌 없음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleVoteCreated(VoteCreatedEvent event) {
        onVoteCreated(event.feed());
    }

    /**
     * 투표 발생 시 작성자에게 마일스톤 알림 (1명, 10명)
     * - guest 투표는 호출하지 않음 (VoteFacade에서 필터링)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVoteCreated(Feed feed) {
        long total = (feed.getYesCount() == null ? 0L : feed.getYesCount())
                   + (feed.getNoCount() == null ? 0L : feed.getNoCount());

        if (total != 1L && total != 10L) return;

        User author = feed.getUser();
        if (author == null) {
            return; // 게스트 작성 피드는 알림 받을 작성자가 없음
        }

        NotificationType type;
        String title;
        String body;

        if (total == 1L) {
            type = NotificationType.MY_FEED_VOTED_1;
            title = buildVotedTitle(feed.getTitle(), 1);
            body = VOTED_1_BODY;
        } else {
            type = NotificationType.MY_FEED_VOTED_10;
            title = buildVotedTitle(feed.getTitle(), 10);
            body = VOTED_10_BODY;
        }

        Notification notification = notificationService.createIfAbsent(author, feed, type, title, body);
        pushBestEffort(author, notification, feed, type);
    }

    /**
     * 답글 작성 커밋 후 호출 — 요청 스레드와 분리된 notificationExecutor에서 실행 (메일 발송 지연이 응답에 영향 없도록)
     */
    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentReplied(CommentRepliedEvent event) {
        try {
            onCommentReplied(event.replyId());
        } catch (Exception e) {
            log.error("답글 알림 처리 실패 replyId={}", event.replyId(), e);
        }
    }

    /**
     * 답글 알림: 원댓글 작성자 + 이 스레드에 답글을 단 회원 전체에게, 답글이 달릴 때마다 발송
     * - 답글 작성자는 회원/게스트 모두 가능. 수신자는 회원만 (게스트는 계정이 없어 받을 수 없음)
     * - 제외: 답글 작성자 본인, 답글 작성자를 차단한 회원
     * - 채널: 푸시 가능 → 푸시 / 푸시 불가(웹 회원) + 이메일 있음 → 이메일(같은 스레드는 30분에 1통) / 그 외 → 알림함 저장만
     * - 수신자마다 트랜잭션/예외를 분리 — 한 명의 실패(저장/발송/로그)가 나머지 수신자 발송을 막지 않는다
     *   (트랜잭션이 없으면 수신자별 새 트랜잭션, 이미 있으면 참여)
     */
    public void onCommentReplied(Long replyId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        ReplyNotificationContext context = tx.execute(status -> buildReplyContext(replyId));
        if (context == null) {
            return;
        }

        for (Long recipientId : context.recipientIds()) {
            try {
                ReplyNotificationTarget target = tx.execute(status -> saveReplyNotification(context, recipientId));
                if (target != null) {
                    deliverReplyNotification(context, target);
                }
            } catch (Exception e) {
                log.error("답글 알림 생성 실패 replyId={}, userId={}", replyId, recipientId, e);
            }
        }
    }

    /**
     * 메일 하단 "이메일 알림 끄기" / List-Unsubscribe 원클릭 처리
     * @return 처리 성공 여부 (토큰이 유효하지 않거나 탈퇴한 회원이면 false)
     */
    @Transactional
    public boolean unsubscribeEmail(String token) {
        Long userId = jwtTokenService.parseEmailUnsubscribeToken(token);
        if (userId == null) {
            return false;
        }
        try {
            userService.disableEmailNotification(userId);
            return true;
        } catch (GlobalException e) {
            return false;
        }
    }

    private ReplyNotificationContext buildReplyContext(Long replyId) {
        Comment reply;
        try {
            reply = commentService.findById(replyId);
        } catch (GlobalException e) {
            return null; // 알림 처리 전에 답글이 삭제됨
        }
        if (!reply.isReply()) {
            return null;
        }

        Comment root = reply.getParent();
        Long replierId = reply.isGuestComment() ? null : reply.getUser().getId();
        Long rootAuthorId = root.isGuestComment() ? null : root.getUser().getId();

        Set<Long> recipientIds = new LinkedHashSet<>();
        if (rootAuthorId != null) {
            recipientIds.add(rootAuthorId);
        }
        recipientIds.addAll(commentService.findDistinctReplyAuthorIds(root.getId()));
        if (replierId != null) {
            // 게스트 답글은 계정이 없어 본인 제외/차단 대상이 아니다 (게스트는 차단할 수 없음)
            recipientIds.remove(replierId);
            userBlockService.findBlockerIds(replierId).forEach(recipientIds::remove);
        }

        Feed feed = reply.getFeed();
        return new ReplyNotificationContext(
                reply.getId(), root.getId(), feed.getId(), feed.getTitle(),
                reply.getDisplayNickname(), reply.getContent(), rootAuthorId, List.copyOf(recipientIds)
        );
    }

    private ReplyNotificationTarget saveReplyNotification(ReplyNotificationContext context, Long recipientId) {
        User recipient = userService.findById(recipientId);
        Feed feed = feedService.findById(context.feedId());

        String title = buildReplyTitle(context.replierNickname(), recipientId.equals(context.rootAuthorId()));
        String body = truncate(context.replyContent(), REPLY_BODY_MAX_LENGTH, "...");

        Notification notification = notificationService.createIfAbsent(
                recipient, feed, NotificationType.COMMENT_REPLIED, context.replyId(), title, body
        );
        return notification == null ? null : new ReplyNotificationTarget(recipient, notification);
    }

    private void deliverReplyNotification(ReplyNotificationContext context, ReplyNotificationTarget target) {
        User user = target.recipient();
        Notification notification = target.notification();

        if (user.canReceivePush()) {
            Map<String, String> data = new HashMap<>();
            data.put("screen", "COMMENT_THREAD");
            data.put("notificationId", String.valueOf(notification.getId()));
            data.put("feedId", String.valueOf(context.feedId()));
            data.put("commentId", String.valueOf(context.rootId()));
            data.put("type", NotificationType.COMMENT_REPLIED.name());
            sendAndRecord(user, context, notification, NotificationChannel.PUSH,
                    () -> fcmSender.send(user.getFcmToken(), notification.getTitle(), notification.getBody(), data));
            return;
        }

        if (user.canReceiveEmail() && emailThrottle.tryAcquire(user.getId(), context.rootId())) {
            String unsubscribeUrl = trimTrailingSlash(emailProperties.apiBaseUrl()) + EMAIL_UNSUBSCRIBE_PATH
                    + jwtTokenService.createEmailUnsubscribeToken(user.getId());
            String feedUrl = trimTrailingSlash(emailProperties.webBaseUrl()) + "/feed/" + context.feedId();
            String html = CommentReplyEmailTemplate.render(
                    notification.getTitle(), context.feedTitle(), context.replierNickname(), context.replyContent(),
                    feedUrl, unsubscribeUrl
            );
            sendAndRecord(user, context, notification, NotificationChannel.EMAIL,
                    () -> emailSender.send(new EmailMessage(user.getEmail(), notification.getTitle(), html, unsubscribeUrl)));
        }
    }

    /**
     * 발송은 best-effort — 실패해도 예외를 밖으로 던지지 않고 발송 로그(push_logs)에 실패로 남긴다
     * (로그에는 이메일 주소를 남기지 않는다)
     */
    private void sendAndRecord(User user, ReplyNotificationContext context, Notification notification,
                               NotificationChannel channel, Runnable send) {
        try {
            send.run();
            pushLogService.record(user.getId(), context.feedId(), NotificationType.COMMENT_REPLIED,
                    notification.getTitle(), notification.getBody(), true, null, channel);
        } catch (Exception e) {
            log.warn("답글 알림 발송 실패 (best-effort). userId={}, replyId={}, channel={}",
                    user.getId(), context.replyId(), channel, e);
            pushLogService.record(user.getId(), context.feedId(), NotificationType.COMMENT_REPLIED,
                    notification.getTitle(), notification.getBody(), false, e.getMessage(), channel);
        }
    }

    /**
     * "'{닉네임}'님이 ..." 형태. notifications.title(60자)을 넘지 않도록 닉네임을 자른다
     */
    private String buildReplyTitle(String replierNickname, boolean toRootAuthor) {
        String suffix = toRootAuthor ? REPLY_TITLE_TO_ROOT_AUTHOR : REPLY_TITLE_TO_PARTICIPANT;
        int maxNicknameLength = NOTIFICATION_TITLE_MAX_LENGTH - suffix.length() - 2; // 따옴표 2개
        String nickname = truncate(replierNickname, maxNicknameLength - 2, "..");
        return "'" + nickname + "'" + suffix;
    }

    private Map<Long, Long> findRootCommentIds(List<Notification> notifications) {
        List<Long> replyIds = notifications.stream()
                .filter(n -> n.getType() == NotificationType.COMMENT_REPLIED)
                .map(Notification::getTargetId)
                .distinct()
                .toList();
        return commentService.findParentIdsByReplyIds(replyIds);
    }

    private String truncate(String value, int maxLength, String ellipsis) {
        if (!StringUtils.hasText(value)) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) + ellipsis : value;
    }

    private String trimTrailingSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record ReplyNotificationContext(
            Long replyId,
            Long rootId,
            Long feedId,
            String feedTitle,
            String replierNickname,
            String replyContent,
            Long rootAuthorId,
            List<Long> recipientIds
    ) {}

    private record ReplyNotificationTarget(User recipient, Notification notification) {}

    private void notifyFeedClosed(Feed feed) {
        User author = feed.getUser();
        String closedTitle = buildClosedTitle(feed.getTitle());

        // 1) 작성자 알림 (게스트 작성 피드는 알림 받을 작성자가 없어 스킵)
        if (author != null) {
            Notification authorNoti = notificationService.createIfAbsent(
                    author, feed, NotificationType.MY_FEED_CLOSED, closedTitle, FEED_CLOSED_BODY
            );
            pushBestEffort(author, authorNoti, feed, NotificationType.MY_FEED_CLOSED);
        }

        // 2) 참여자 알림 (guest 제외, 작성자 제외)
        List<Long> participantIds = voteLogService.findDistinctUserIdsVotedByFeedId(feed.getId());
        Long authorId = author != null ? author.getId() : null;
        participantIds = participantIds.stream()
                .filter(uid -> !uid.equals(authorId))
                .toList();

        if (participantIds.isEmpty()) {
            return;
        }

        List<User> participants = userService.findByIds(participantIds);

        for (User u : participants) {
            Notification saved = notificationService.createIfAbsent(
                    u, feed, NotificationType.PARTICIPATED_FEED_CLOSED, closedTitle, FEED_CLOSED_BODY
            );
            pushBestEffort(u, saved, feed, NotificationType.PARTICIPATED_FEED_CLOSED);
        }
    }

    /**
     * Push는 best-effort (실패해도 트랜잭션 롤백 X)
     * - user 조건 충족 + notification이 실제 "신규 생성"된 경우만 보낸다.
     */
    private void pushBestEffort(User user, Notification notification, Feed feed, NotificationType type) {
        if (notification == null) return; // 중복이면 null 반환
        if (!user.canReceivePush()) return;

        try {
            Map<String, String> data = new HashMap<>();
            data.put("screen", "FEED_DETAIL");
            data.put("notificationId", String.valueOf(notification.getId()));
            data.put("feedId", String.valueOf(feed.getId()));
            data.put("type", type.name());

            fcmSender.send(user.getFcmToken(), notification.getTitle(), notification.getBody(), data);
//            log.info("FCM 전송 성공 userId={}, token={}", user.getId(), user.getFcmToken());
            pushLogService.record(user.getId(), feed.getId(), type, notification.getTitle(), notification.getBody(), true, null);
        } catch (Exception e) {
            log.warn("FCM 전송 실패 (best-effort). userId={}, feedId={}, type={}",
                    user.getId(), feed.getId(), type, e);
            pushLogService.record(user.getId(), feed.getId(), type, notification.getTitle(), notification.getBody(), false, e.getMessage());
        }
    }

    /**
     * 피드 투표 결과 계산 (Feed.yesCount / Feed.noCount 기준)
     * - voteType과 무관하게 카운트 기준으로 계산한다.
     */
    private NotificationResultCommand calculateResult(Feed feed) {
        if (feed == null) {
            // 데이터 정합성 문제 (이중 안전) → 도메인 에러코드로 통일
            throw new GlobalException(NotificationErrorCode.NOTIFICATION_FEED_NOT_FOUND);
        }

        long yes = feed.getYesCount() == null ? 0L : feed.getYesCount();
        long no = feed.getNoCount() == null ? 0L : feed.getNoCount();
        long total = yes + no;

        // 0표
        if (total == 0) {
            return new NotificationResultCommand(0, "ZERO");
        }

        // 동률
        if (yes == no) {
            return new NotificationResultCommand(50, "TIE");
        }

        boolean yesWin = yes > no;
        long winCnt = yesWin ? yes : no;

        int percent = (int) Math.round(winCnt * 100.0 / total);
        String label = yesWin ? "YES" : "NO";

        return new NotificationResultCommand(percent, label);
    }

    private String truncateTitle(String title) {
        if (!StringUtils.hasText(title)) return "";
        return title.length() > FEED_TITLE_MAX_LENGTH
                ? title.substring(0, FEED_TITLE_MAX_LENGTH) + ".."
                : title;
    }

    private String buildVotedTitle(String feedTitle, int count) {
        return "'" + truncateTitle(feedTitle) + "' 투표 " + count + "명 참여!";
    }

    private String buildClosedTitle(String feedTitle) {
        return "'" + truncateTitle(feedTitle) + "' 투표 종료!";
    }

    /**
     * CloudFront 조회 URL 생성
     * - domain: xxx.buy-or-not.com/
     * - s3Key: feeds/uuid_xxx.jpg
     */
    private String buildCloudFrontUrl(String domain, String s3Key) {
        if (!StringUtils.hasText(domain) || !StringUtils.hasText(s3Key)) {
            return null;
        }
        String d = domain.trim();
        if (d.endsWith("/")) d = d.substring(0, d.length() - 1);

        String key = s3Key.startsWith("/") ? s3Key.substring(1) : s3Key;
        return d + "/" + key;
    }

    public void sendTestPushOnly(User user, String title, String body) {

        if (!user.canReceivePush()) {
//            log.warn("테스트 푸시 불가: userId={}, pushEnabled={}, token={}",
//                    user.getId(), user.isPushEnabled(), user.getFcmToken());
            return;
        }

        try {
            Map<String, String> data = new HashMap<>();
            data.put("screen", "NOTIFICATIONS");
            data.put("type", "TEST");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            fcmSender.send(user.getFcmToken(), title, body, data);

            log.info("테스트 FCM 전송 성공 userId={}", user.getId());
            pushLogService.record(user.getId(), null, null, title, body, true, null);

        } catch (Exception e) {
            log.warn("테스트 FCM 전송 실패 userId={}", user.getId(), e);
            pushLogService.record(user.getId(), null, null, title, body, false, e.getMessage());
        }
    }
}
