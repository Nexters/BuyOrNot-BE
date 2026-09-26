package com.nexters.sseotdabwa.api.comments.facade;

import java.util.List;
import java.util.Map;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.comments.exception.CommentErrorCode;
import com.nexters.sseotdabwa.domain.comments.service.CommentProfanityFilter;
import com.nexters.sseotdabwa.domain.comments.service.CommentProfanityViolationTracker;
import com.nexters.sseotdabwa.domain.comments.service.CommentRateLimiter;
import com.nexters.sseotdabwa.domain.comments.service.CommentService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.DefaultProfileImage;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글(의견) 생성/조회/삭제/신고 흐름 조합 Facade
 * - 투표 여부와 무관하게 회원/게스트 모두 댓글 작성 가능. 투표는 태그/버블(voteChoice) 노출에만 영향
 */
@Component
@RequiredArgsConstructor
public class CommentFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;
    private final CommentService commentService;
    private final VoteLogService voteLogService;
    private final AwsProperties awsProperties;
    private final PasswordEncoder passwordEncoder;
    private final CommentRateLimiter commentRateLimiter;
    private final CommentProfanityFilter commentProfanityFilter;
    private final CommentProfanityViolationTracker commentProfanityViolationTracker;

    /**
     * 회원 댓글 작성
     * - 요청 빈도 제한은 계정(userId) 기준. 웹 회원 요청은 Cloudflare Worker를 거쳐 들어와 IP가 전부 동일하게
     *   보이므로 IP 기준으로는 제한할 수 없다(게스트 경로와의 차이점).
     */
    @Transactional
    public CommentCreateResponse createComment(User user, Long feedId, CommentCreateRequest request) {
        if (commentRateLimiter.isExceededForMember(user.getId())) {
            throw new GlobalException(CommentErrorCode.COMMENT_RATE_LIMIT_EXCEEDED);
        }
        String identity = memberIdentity(user);
        validateNotProfane(identity, request.content());
        Feed feed = feedService.findByIdWithLock(feedId);
        validateFeedOpen(feed);

        Comment comment = commentService.createMemberComment(feed, user, request.content());
        return CommentCreateResponse.of(comment.getId());
    }

    /**
     * 게스트 댓글 작성
     * - 비밀번호는 해시로 저장, 이후 삭제 시 본인 확인에 사용 (Feed 게스트 작성과 동일 패턴)
     */
    @Transactional
    public CommentCreateResponse createGuestComment(Long feedId, CommentCreateRequestGuest request, String ip, String deviceId) {
        validateRateLimit(ip, deviceId);
        String identity = guestIdentity(ip, deviceId);
        validateNotProfane(identity, request.content());
        Feed feed = feedService.findByIdWithLock(feedId);
        validateFeedOpen(feed);

        String guestProfileImage = randomGuestProfileImageUrl();
        Comment comment = commentService.createGuestComment(
                feed,
                request.guestNickname(),
                passwordEncoder.encode(request.guestPassword()),
                guestProfileImage,
                request.content()
        );
        return CommentCreateResponse.of(comment.getId());
    }

    /**
     * 회원 댓글 삭제 (본인 소유만)
     */
    @Transactional
    public void deleteComment(User user, Long commentId) {
        Comment comment = commentService.findById(commentId);
        if (!comment.isOwner(user)) {
            throw new GlobalException(CommentErrorCode.COMMENT_DELETE_FORBIDDEN);
        }
        commentService.delete(comment);
    }

    /**
     * 게스트 댓글 삭제 (비밀번호 일치 시에만)
     */
    @Transactional
    public void deleteGuestComment(Long commentId, CommentGuestDeleteRequest request) {
        Comment comment = commentService.findById(commentId);
        if (!comment.isGuestComment()) {
            throw new GlobalException(CommentErrorCode.COMMENT_NOT_GUEST_COMMENT);
        }
        if (!passwordEncoder.matches(request.password(), comment.getGuestPasswordHash())) {
            throw new GlobalException(CommentErrorCode.COMMENT_GUEST_PASSWORD_MISMATCH);
        }
        commentService.delete(comment);
    }

    /**
     * 댓글 신고 (회원/게스트 모두 가능, 인증 불필요)
     * - 본인 댓글 자기신고는 로그인 유저에 한해 방지(게스트는 신원 식별이 불가해 검사 대상 아님)
     * - 1건 신고로 즉시 숨김(누적 임계치 없음, Feed 신고와 동일 정책)
     */
    @Transactional
    public void reportComment(User user, Long commentId) {
        Comment comment = commentService.findById(commentId);
        if (user != null && comment.isOwner(user)) {
            throw new GlobalException(CommentErrorCode.COMMENT_SELF_REPORT);
        }
        if (comment.isReported()) {
            throw new GlobalException(CommentErrorCode.COMMENT_ALREADY_REPORTED);
        }
        commentService.report(comment);
    }

    /**
     * 댓글 목록 조회 (등록순/최신순, 커서 기반 페이지네이션)
     * - 인증 없이도 조회 가능하므로 user는 null일 수 있다 (그 경우 isMine은 항상 false)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponse> getComments(User user, Long feedId, Long cursor, Integer size, CommentSort sort) {
        feedService.findById(feedId);
        int pageSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        CommentSort effectiveSort = (sort == null) ? CommentSort.REGISTERED : sort;

        List<Comment> comments = commentService.findByFeedIdWithCursor(feedId, cursor, pageSize, effectiveSort);

        boolean hasNext = comments.size() > pageSize;
        List<Comment> slicedComments = hasNext ? comments.subList(0, pageSize) : comments;

        List<Long> memberAuthorIds = slicedComments.stream()
                .filter(c -> !c.isGuestComment())
                .map(c -> c.getUser().getId())
                .distinct()
                .toList();
        Map<Long, VoteChoice> voteChoiceByUserId = voteLogService.findChoicesByFeedIdAndUserIds(feedId, memberAuthorIds);

        List<CommentResponse> content = slicedComments.stream()
                .map(comment -> {
                    VoteChoice voteChoice = comment.isGuestComment() ? null : voteChoiceByUserId.get(comment.getUser().getId());
                    return CommentResponse.of(comment, user, resolveProfileImage(comment), voteChoice);
                })
                .toList();

        Long nextCursor = hasNext ? slicedComments.get(slicedComments.size() - 1).getId() : null;
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    private void validateFeedOpen(Feed feed) {
        if (feed.isExpired() || !feed.isVoteOpen()) {
            throw new GlobalException(CommentErrorCode.COMMENT_FEED_CLOSED);
        }
    }

    private void validateRateLimit(String ip, String deviceId) {
        if (commentRateLimiter.isExceeded(ip, deviceId)) {
            throw new GlobalException(CommentErrorCode.COMMENT_RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 금칙어 위반 누적으로 일시 차단된 상태면 내용을 검사하지도 않고 즉시 429로 막는다.
     * 아니라면 이번 내용이 금칙어를 포함하는지 검사하고, 포함 시 위반을 기록한 뒤 400을 반환한다.
     */
    private void validateNotProfane(String identity, String content) {
        if (commentProfanityViolationTracker.isBlocked(identity)) {
            throw new GlobalException(CommentErrorCode.COMMENT_TEMPORARILY_RESTRICTED);
        }
        if (commentProfanityFilter.containsProfanity(content)) {
            commentProfanityViolationTracker.recordViolation(identity);
            throw new GlobalException(CommentErrorCode.COMMENT_PROFANITY_DETECTED);
        }
    }

    private String memberIdentity(User user) {
        return "user:" + user.getId();
    }

    private String guestIdentity(String ip, String deviceId) {
        return "guest:" + ip + ":" + (deviceId == null ? "" : deviceId);
    }

    private String resolveProfileImage(Comment comment) {
        return comment.isGuestComment() ? comment.getGuestProfileImage() : comment.getUser().getProfileImage();
    }

    /**
     * 게스트 댓글 작성 시 한 번만 부여되는 랜덤 기본 프로필 이미지 URL
     * - 게스트 피드 작성 시 부여 방식과 동일 (CloudFront 도메인 + 파일명)
     */
    private String randomGuestProfileImageUrl() {
        final String domain = awsProperties.cloudfront().domain().replaceAll("/$", "");
        return domain + "/" + DefaultProfileImage.randomFileName();
    }
}
