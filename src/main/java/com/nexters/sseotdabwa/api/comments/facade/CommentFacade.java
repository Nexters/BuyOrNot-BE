package com.nexters.sseotdabwa.api.comments.facade;

import java.util.List;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.exception.CommentErrorCode;
import com.nexters.sseotdabwa.domain.comments.service.CommentService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.service.FeedService;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.DefaultProfileImage;
import com.nexters.sseotdabwa.domain.votes.service.VoteLogService;
import com.nexters.sseotdabwa.domain.votes.service.VoteTokenService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글(의견) 생성/조회 흐름 조합 Facade
 * - 투표한 회원/게스트만 댓글 작성 가능 (Phase1: 작성/조회만 지원, 수정/삭제/신고는 Phase2)
 */
@Component
@RequiredArgsConstructor
public class CommentFacade {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final FeedService feedService;
    private final CommentService commentService;
    private final VoteLogService voteLogService;
    private final VoteTokenService voteTokenService;
    private final AwsProperties awsProperties;

    /**
     * 회원 댓글 작성
     * - 투표 여부는 클라이언트를 신뢰하지 않고 서버가 DB로 재검증한다.
     */
    @Transactional
    public CommentCreateResponse createComment(User user, Long feedId, CommentCreateRequest request) {
        Feed feed = feedService.findByIdWithLock(feedId);
        validateFeedOpen(feed);

        if (!voteLogService.existsByUserAndFeed(user.getId(), feedId)) {
            throw new GlobalException(CommentErrorCode.COMMENT_NOT_VOTED);
        }

        Comment comment = commentService.createMemberComment(feed, user, request.content());
        return CommentCreateResponse.of(comment.getId());
    }

    /**
     * 게스트 댓글 작성
     * - 게스트는 세션이 없어 DB로 투표 여부를 확인할 수 없으므로, 투표 응답으로 받은 voteToken으로 자격을 증명한다.
     */
    @Transactional
    public CommentCreateResponse createGuestComment(Long feedId, CommentCreateRequestGuest request) {
        Feed feed = feedService.findByIdWithLock(feedId);
        validateFeedOpen(feed);

        if (!voteTokenService.isValid(request.voteToken(), feedId)) {
            throw new GlobalException(CommentErrorCode.COMMENT_INVALID_VOTE_TOKEN);
        }

        String guestProfileImage = randomGuestProfileImageUrl();
        Comment comment = commentService.createGuestComment(feed, request.guestNickname(), guestProfileImage, request.content());
        return CommentCreateResponse.of(comment.getId());
    }

    /**
     * 댓글 목록 조회 (등록순, 커서 기반 페이지네이션)
     * - 인증 없이도 조회 가능하므로 user는 null일 수 있다 (그 경우 isMine은 항상 false)
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<CommentResponse> getComments(User user, Long feedId, Long cursor, Integer size) {
        feedService.findById(feedId);
        int pageSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        List<Comment> comments = commentService.findByFeedIdWithCursor(feedId, cursor, pageSize);

        boolean hasNext = comments.size() > pageSize;
        List<Comment> slicedComments = hasNext ? comments.subList(0, pageSize) : comments;

        List<CommentResponse> content = slicedComments.stream()
                .map(comment -> CommentResponse.of(comment, user, resolveProfileImage(comment)))
                .toList();

        Long nextCursor = hasNext ? slicedComments.get(slicedComments.size() - 1).getId() : null;
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    private void validateFeedOpen(Feed feed) {
        if (feed.isExpired() || !feed.isVoteOpen()) {
            throw new GlobalException(CommentErrorCode.COMMENT_FEED_CLOSED);
        }
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
