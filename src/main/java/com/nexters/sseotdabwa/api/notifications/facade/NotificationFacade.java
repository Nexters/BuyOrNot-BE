package com.nexters.sseotdabwa.api.notifications.facade;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.api.notifications.exception.NotificationErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.notifications.dto.NotificationResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.service.FeedImageService;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.notifications.entity.Notification;
import com.nexters.sseotdabwa.domain.notifications.enums.NotificationType;
import com.nexters.sseotdabwa.domain.notifications.push.FcmSender;
import com.nexters.sseotdabwa.domain.notifications.service.NotificationService;
import com.nexters.sseotdabwa.domain.notifications.service.command.NotificationResultCommand;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.service.UserService;
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

    private static final String FEED_CLOSED_TITLE = "투표 종료!";
    private static final String FEED_CLOSED_BODY = "토봉이가 결과를 들고 기다리고 있어요.";

    private final NotificationService notificationService;
    private final FeedService feedService;
    private final FeedImageService feedImageService;
    private final VoteLogService voteLogService;
    private final UserService userService;

    private final FcmSender fcmSender;
    private final AwsProperties awsProperties;

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
                .collect(Collectors.toMap(fi -> fi.getFeed().getId(), fi -> fi));

        // 4) 알림 -> 응답 매핑
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
                            viewUrl
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
            notifyFeedClosed(feed);
        }
    }

    private void notifyFeedClosed(Feed feed) {
        User author = feed.getUser();

        // 1) 작성자 알림
        Notification authorNoti = notificationService.createIfAbsent(
                author, feed, NotificationType.MY_FEED_CLOSED, FEED_CLOSED_TITLE, FEED_CLOSED_BODY
        );
        pushBestEffort(author, authorNoti, feed, NotificationType.MY_FEED_CLOSED);

        // 2) 참여자 알림 (guest 제외, 작성자 제외)
        List<Long> participantIds = voteLogService.findDistinctUserIdsVotedByFeedId(feed.getId());
        participantIds = participantIds.stream()
                .filter(uid -> !uid.equals(author.getId()))
                .toList();

        if (participantIds.isEmpty()) {
            return;
        }

        List<User> participants = userService.findByIds(participantIds);

        for (User u : participants) {
            Notification saved = notificationService.createIfAbsent(
                    u, feed, NotificationType.PARTICIPATED_FEED_CLOSED, FEED_CLOSED_TITLE, FEED_CLOSED_BODY
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
            data.put("screen", "NOTIFICATIONS");
            data.put("notificationId", String.valueOf(notification.getId()));
            data.put("feedId", String.valueOf(feed.getId()));
            data.put("type", type.name());

            fcmSender.send(user.getFcmToken(), FEED_CLOSED_TITLE, FEED_CLOSED_BODY, data);
//            log.info("FCM 전송 성공 userId={}, token={}", user.getId(), user.getFcmToken());
        } catch (Exception e) {
            log.warn("FCM 전송 실패 (best-effort). userId={}, feedId={}, type={}",
                    user.getId(), feed.getId(), type, e);
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

    @Transactional(readOnly = true)
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

        } catch (Exception e) {
            log.warn("테스트 FCM 전송 실패 userId={}", user.getId(), e);
        }
    }
}
