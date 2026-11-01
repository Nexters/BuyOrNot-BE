package com.nexters.sseotdabwa.api.comments.facade;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentLikeResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentReplyResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentThreadResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.comments.event.CommentRepliedEvent;
import com.nexters.sseotdabwa.domain.comments.exception.CommentErrorCode;
import com.nexters.sseotdabwa.domain.comments.service.CommentLikeService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글(의견)/답글/공감 생성·조회·삭제·신고 흐름 조합 Facade
 * - 투표 여부와 무관하게 회원/게스트 모두 원댓글 작성 가능. 투표는 태그/버블(voteChoice) 노출에만 영향
 * - 답글은 회원/게스트 모두 가능, 공감은 회원만 가능
 * - 마감된 피드에도 댓글/답글/공감 가능
 */
@Component
@RequiredArgsConstructor
public class CommentFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;
    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final VoteLogService voteLogService;
    private final AwsProperties awsProperties;
    private final PasswordEncoder passwordEncoder;
    private final CommentRateLimiter commentRateLimiter;
    private final CommentProfanityFilter commentProfanityFilter;
    private final CommentProfanityViolationTracker commentProfanityViolationTracker;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원 댓글 작성
     * - 요청 빈도 제한은 계정(userId) 기준. 웹 회원 요청은 Cloudflare Worker를 거쳐 들어와 IP가 전부 동일하게
     *   보이므로 IP 기준으로는 제한할 수 없다(게스트 경로와의 차이점).
     */
    @Transactional
    public CommentCreateResponse createComment(User user, Long feedId, CommentCreateRequest request) {
        validateMemberRequest(user, request.content());
        Feed feed = feedService.findByIdWithLock(feedId);

        Comment comment = commentService.createMemberComment(feed, user, request.content());
        return CommentCreateResponse.of(comment.getId());
    }

    /**
     * 회원 답글 작성
     * - 원댓글에만 달 수 있고(답글에 답글 불가), 신고/삭제된 원댓글에는 달 수 없다
     * - 요청 빈도 제한/금칙어는 원댓글과 같은 카운터를 공유한다
     * - 커밋 후 답글 알림(푸시/이메일)이 비동기로 발송된다
     */
    @Transactional
    public CommentCreateResponse createReply(User user, Long feedId, Long commentId, CommentCreateRequest request) {
        validateMemberRequest(user, request.content());
        Feed feed = feedService.findByIdWithLock(feedId);

        Comment parent = findReplyableParent(feedId, commentId);

        Comment reply = commentService.createMemberReply(feed, parent, user, request.content());
        eventPublisher.publishEvent(new CommentRepliedEvent(reply.getId()));
        return CommentCreateResponse.of(reply.getId());
    }

    /**
     * 게스트 답글 작성
     * - 검증/저장 방식은 게스트 원댓글과 동일 (IP·디바이스 기준 빈도 제한, 금칙어, 피드 내 닉네임 중복 재발급, 비밀번호 해시 저장)
     * - 부모 조건은 회원 답글과 동일 (원댓글에만, 신고/삭제된 원댓글 불가)
     * - 삭제는 게스트 댓글 삭제 API(비밀번호 확인)로 한다
     */
    @Transactional
    public CommentCreateResponse createGuestReply(Long feedId, Long commentId, CommentCreateRequestGuest request,
                                                  String ip, String deviceId) {
        validateRateLimit(ip, deviceId);
        validateNotProfane(guestIdentity(ip, deviceId), request.content());
        Feed feed = feedService.findByIdWithLock(feedId);
        Comment parent = findReplyableParent(feedId, commentId);

        Comment reply = commentService.createGuestReply(
                feed,
                parent,
                request.guestNickname(),
                passwordEncoder.encode(request.guestPassword()),
                randomGuestProfileImageUrl(),
                request.content()
        );
        eventPublisher.publishEvent(new CommentRepliedEvent(reply.getId()));
        return CommentCreateResponse.of(reply.getId());
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
     * 회원 댓글/답글 삭제 (본인 소유만). 답글이 남아있는 원댓글은 "삭제된 댓글"로 남는다.
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
     * 게스트 댓글 삭제 (비밀번호 일치 시에만). 답글이 남아있으면 "삭제된 댓글"로 남는다.
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
     * - 1건 신고로 즉시 숨김(누적 임계치 없음, Feed 신고와 동일 정책). 신고된 원댓글은 답글까지 함께 가려진다
     */
    @Transactional
    public void reportComment(User user, Long commentId) {
        Comment comment = commentService.findById(commentId);
        if (comment.isDeleted()) {
            throw new GlobalException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        if (user != null && comment.isOwner(user)) {
            throw new GlobalException(CommentErrorCode.COMMENT_SELF_REPORT);
        }
        if (comment.isReported()) {
            throw new GlobalException(CommentErrorCode.COMMENT_ALREADY_REPORTED);
        }
        commentService.report(comment);
    }

    /**
     * 공감 (회원 전용, 멱등). 원댓글에만 가능, 본인 댓글도 가능
     * - 같은 댓글에 대한 동시 요청은 댓글 행 잠금으로 직렬화해 중복 공감을 막는다
     */
    @Transactional
    public CommentLikeResponse likeComment(User user, Long feedId, Long commentId) {
        Comment comment = findLikeTargetWithLock(feedId, commentId);
        if (comment.isReported() || comment.isDeleted()) {
            throw new GlobalException(CommentErrorCode.COMMENT_LIKE_UNAVAILABLE);
        }
        commentLikeService.like(comment, user);
        return CommentLikeResponse.of(commentLikeService.countByCommentId(comment.getId()), true);
    }

    /**
     * 공감 취소 (회원 전용, 멱등)
     */
    @Transactional
    public CommentLikeResponse unlikeComment(User user, Long feedId, Long commentId) {
        Comment comment = findLikeTargetWithLock(feedId, commentId);
        commentLikeService.unlike(comment, user);
        return CommentLikeResponse.of(commentLikeService.countByCommentId(comment.getId()), false);
    }

    /**
     * 원댓글 목록 조회 (등록순/최신순, 커서 기반 페이지네이션) — 각 원댓글에 답글 전체(등록순)를 붙여서 내려준다
     * - 인증 없이도 조회 가능하므로 user는 null일 수 있다 (그 경우 isMine/isLiked는 항상 false)
     * - 페이지 크기는 원댓글 기준이며, 답글/공감/투표 정보는 원댓글 id 목록으로 배치 조회한다 (N+1 방지)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponse> getComments(User user, Long feedId, Long cursor, Integer size, CommentSort sort) {
        feedService.findById(feedId);
        int pageSize = resolvePageSize(size);
        CommentSort effectiveSort = (sort == null) ? CommentSort.REGISTERED : sort;

        List<Comment> comments = commentService.findByFeedIdWithCursor(feedId, cursor, pageSize, effectiveSort);

        boolean hasNext = comments.size() > pageSize;
        List<Comment> roots = hasNext ? comments.subList(0, pageSize) : comments;

        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParentId = commentService.findRepliesByParentIds(rootIds);
        Map<Long, Long> likeCountByCommentId = commentLikeService.countByCommentIds(rootIds);
        Set<Long> likedCommentIds = commentLikeService.findLikedCommentIds(user, rootIds);
        Map<Long, VoteChoice> voteChoiceByUserId = findRootAuthorVoteChoices(feedId, roots);

        List<CommentResponse> content = roots.stream()
                .map(root -> {
                    List<CommentReplyResponse> replies = repliesByParentId.getOrDefault(root.getId(), List.of()).stream()
                            .map(reply -> CommentReplyResponse.of(reply, user))
                            .toList();
                    VoteChoice voteChoice = root.isGuestComment() ? null : voteChoiceByUserId.get(root.getUser().getId());
                    return CommentResponse.of(
                            root,
                            user,
                            resolveProfileImage(root),
                            voteChoice,
                            likeCountByCommentId.getOrDefault(root.getId(), 0L),
                            likedCommentIds.contains(root.getId()),
                            replies.size(),
                            replies
                    );
                })
                .toList();

        Long nextCursor = hasNext ? roots.get(roots.size() - 1).getId() : null;
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    /**
     * 답글 페이지 상단 고정용 원댓글 + 헤더 카운트(원댓글 1 + 답글 수)
     */
    @Transactional(readOnly = true)
    public CommentThreadResponse getCommentThread(User user, Long feedId, Long commentId) {
        Comment root = findVisibleRoot(feedId, commentId);

        VoteChoice voteChoice = root.isGuestComment()
                ? null
                : voteLogService.findChoicesByFeedIdAndUserIds(feedId, List.of(root.getUser().getId())).get(root.getUser().getId());

        CommentResponse comment = CommentResponse.of(
                root,
                user,
                resolveProfileImage(root),
                voteChoice,
                commentLikeService.countByCommentId(root.getId()),
                commentLikeService.isLiked(root.getId(), user),
                commentService.countVisibleReplies(root.getId()),
                List.of()
        );
        return CommentThreadResponse.of(comment);
    }

    /**
     * 답글 페이지의 답글 목록 (등록순 고정, 커서 기반 페이지네이션)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<CommentReplyResponse> getReplies(User user, Long feedId, Long commentId, Long cursor, Integer size) {
        Comment root = findVisibleRoot(feedId, commentId);
        int pageSize = resolvePageSize(size);

        List<Comment> replies = commentService.findRepliesWithCursor(root.getId(), cursor, pageSize);

        boolean hasNext = replies.size() > pageSize;
        List<Comment> sliced = hasNext ? replies.subList(0, pageSize) : replies;

        List<CommentReplyResponse> content = sliced.stream()
                .map(reply -> CommentReplyResponse.of(reply, user))
                .toList();

        Long nextCursor = hasNext ? sliced.get(sliced.size() - 1).getId() : null;
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    private void validateMemberRequest(User user, String content) {
        if (commentRateLimiter.isExceededForMember(user.getId())) {
            throw new GlobalException(CommentErrorCode.COMMENT_RATE_LIMIT_EXCEEDED);
        }
        validateNotProfane(memberIdentity(user), content);
    }

    /**
     * 답글 페이지 대상 원댓글 조회 — 답글이거나, 신고됐거나, 다른 피드 소속이면 404
     * (soft delete된 원댓글은 "삭제된 댓글"로 보여줘야 하므로 허용)
     */
    private Comment findVisibleRoot(Long feedId, Long commentId) {
        Comment comment = commentService.findById(commentId);
        if (!comment.getFeed().getId().equals(feedId) || comment.isReply() || comment.isReported()) {
            throw new GlobalException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    /**
     * 답글을 달 수 있는 원댓글 조회 — 다른 피드 소속이면 404, 답글이면 COMMENT_013, 신고/삭제됐으면 COMMENT_014
     */
    private Comment findReplyableParent(Long feedId, Long commentId) {
        Comment parent = commentService.findById(commentId);
        validateBelongsToFeed(parent, feedId);
        if (parent.isReply()) {
            throw new GlobalException(CommentErrorCode.COMMENT_REPLY_TO_REPLY);
        }
        if (parent.isReported() || parent.isDeleted()) {
            throw new GlobalException(CommentErrorCode.COMMENT_INVALID_PARENT);
        }
        return parent;
    }

    private Comment findLikeTargetWithLock(Long feedId, Long commentId) {
        Comment comment = commentService.findByIdWithLock(commentId);
        validateBelongsToFeed(comment, feedId);
        if (comment.isReply()) {
            throw new GlobalException(CommentErrorCode.COMMENT_LIKE_ON_REPLY);
        }
        return comment;
    }

    private void validateBelongsToFeed(Comment comment, Long feedId) {
        if (!comment.getFeed().getId().equals(feedId)) {
            throw new GlobalException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private Map<Long, VoteChoice> findRootAuthorVoteChoices(Long feedId, List<Comment> roots) {
        List<Long> memberAuthorIds = roots.stream()
                .filter(c -> !c.isGuestComment())
                .map(c -> c.getUser().getId())
                .distinct()
                .toList();
        return voteLogService.findChoicesByFeedIdAndUserIds(feedId, memberAuthorIds);
    }

    private int resolvePageSize(Integer size) {
        return (size == null) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
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
