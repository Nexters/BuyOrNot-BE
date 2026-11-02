package com.nexters.sseotdabwa.domain.comments.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.comments.exception.CommentErrorCode;
import com.nexters.sseotdabwa.domain.comments.repository.CommentLikeRepository;
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.service.RandomNicknameGenerator;

import lombok.RequiredArgsConstructor;

/**
 * 댓글(의견) 도메인 서비스
 * - 호출하는 쪽(Facade)이 이미 Feed를 findByIdWithLock으로 잠근 상태에서 호출한다고 가정한다.
 *   (같은 락 구간 안에서 피드 내 닉네임 중복 검사까지 안전하게 처리하기 위함)
 *
 * 정책:
 * - content: 300자 이하
 * - 답글은 원댓글에만 달 수 있다 (depth 1)
 * - 원댓글을 지우면 답글/공감까지 함께 삭제, 답글을 지우면 해당 답글만 삭제
 * - parent_id self FK 때문에 일괄 삭제는 항상 공감 → 답글 → 원댓글 순
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 300;
    private static final int MAX_NICKNAME_RETRY = 5;

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final RandomNicknameGenerator randomNicknameGenerator;

    @Transactional
    public Comment createMemberComment(Feed feed, User user, String content) {
        String normalized = validateAndNormalizeContent(content);

        Comment comment = Comment.builder()
                .feed(feed)
                .user(user)
                .displayNickname(user.getNickname())
                .content(normalized)
                .build();
        return commentRepository.save(comment);
    }

    /**
     * 회원 답글 작성. 부모 검증(같은 피드/원댓글/신고 여부)은 호출하는 쪽에서 끝낸 상태여야 한다.
     */
    @Transactional
    public Comment createMemberReply(Feed feed, Comment parent, User user, String content) {
        String normalized = validateAndNormalizeContent(content);

        Comment reply = Comment.builder()
                .feed(feed)
                .user(user)
                .parent(parent)
                .displayNickname(user.getNickname())
                .content(normalized)
                .build();
        return commentRepository.save(reply);
    }

    @Transactional
    public Comment createGuestComment(Feed feed, String requestedNickname, String guestPasswordHash, String guestProfileImage, String content) {
        return saveGuestComment(feed, null, requestedNickname, guestPasswordHash, guestProfileImage, content);
    }

    /**
     * 게스트 답글 작성. 부모 검증(같은 피드/원댓글/신고 여부)은 호출하는 쪽에서 끝낸 상태여야 한다.
     * 닉네임/비밀번호/프로필 이미지 처리는 게스트 원댓글과 동일.
     */
    @Transactional
    public Comment createGuestReply(Feed feed, Comment parent, String requestedNickname, String guestPasswordHash,
                                    String guestProfileImage, String content) {
        return saveGuestComment(feed, parent, requestedNickname, guestPasswordHash, guestProfileImage, content);
    }

    private Comment saveGuestComment(Feed feed, Comment parent, String requestedNickname, String guestPasswordHash,
                                     String guestProfileImage, String content) {
        String normalized = validateAndNormalizeContent(content);
        String nickname = resolveUniqueGuestNickname(feed.getId(), requestedNickname);

        Comment comment = Comment.builder()
                .feed(feed)
                .user(null)
                .parent(parent)
                .guestNickname(nickname)
                .guestPasswordHash(guestPasswordHash)
                .guestProfileImage(guestProfileImage)
                .displayNickname(nickname)
                .content(normalized)
                .build();
        return commentRepository.save(comment);
    }

    /**
     * 같은 피드(feedId) 안에서만 닉네임이 중복되지 않도록 함 (전체 서비스 유니크 아님).
     * 클라이언트 조작 방지를 위해 실제 단어 조합인지도 재검증한다.
     */
    private String resolveUniqueGuestNickname(Long feedId, String requestedNickname) {
        String nickname = randomNicknameGenerator.isValid(requestedNickname)
                ? requestedNickname
                : randomNicknameGenerator.generate();

        int attempt = 0;
        while (commentRepository.existsByFeedIdAndDisplayNickname(feedId, nickname) && attempt < MAX_NICKNAME_RETRY) {
            nickname = randomNicknameGenerator.generate();
            attempt++;
        }
        return nickname;
    }

    private String validateAndNormalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new GlobalException(CommentErrorCode.COMMENT_CONTENT_REQUIRED);
        }
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new GlobalException(CommentErrorCode.COMMENT_CONTENT_TOO_LONG);
        }
        return normalized;
    }

    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * 공감 처리처럼 같은 댓글에 대한 동시 요청을 직렬화해야 할 때 사용 (행 잠금)
     */
    @Transactional
    public Comment findByIdWithLock(Long commentId) {
        return commentRepository.findByIdWithLock(commentId)
                .orElseThrow(() -> new GlobalException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * 피드 내 원댓글 목록 (커서 기반 페이지네이션, 등록순/최신순)
     */
    public List<Comment> findByFeedIdWithCursor(Long feedId, Long cursor, int size, CommentSort sort) {
        var pageable = PageRequest.ofSize(size + 1);
        return sort == CommentSort.LATEST
                ? commentRepository.findByFeedIdWithCursorDesc(feedId, cursor, pageable)
                : commentRepository.findByFeedIdWithCursorAsc(feedId, cursor, pageable);
    }

    /**
     * 원댓글 id별 (비신고) 답글 전체, 등록순
     */
    public Map<Long, List<Comment>> findRepliesByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.findRepliesByParentIds(parentIds).stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
    }

    /**
     * 답글 페이지용 답글 목록 (등록순, 커서 기반, size+1개 조회)
     */
    public List<Comment> findRepliesWithCursor(Long parentId, Long cursor, int size) {
        return commentRepository.findRepliesWithCursor(parentId, cursor, PageRequest.ofSize(size + 1));
    }

    public long countVisibleReplies(Long parentId) {
        return commentRepository.countByParentIdAndReportStatusNot(parentId, ReportStatus.REPORTED);
    }

    /**
     * 답글 id → 원댓글 id. 이미 삭제된 답글은 map에 없음
     */
    public Map<Long, Long> findParentIdsByReplyIds(List<Long> replyIds) {
        if (replyIds == null || replyIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.findParentIdsByReplyIds(replyIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public List<Long> findDistinctReplyAuthorIds(Long parentId) {
        return commentRepository.findDistinctReplyAuthorIds(parentId);
    }

    /**
     * feedId별 (댓글 수 + 최신 원댓글 1개) 집계 — 피드 목록 응답 확장(commentCount/latestComment)용.
     * - commentCount: 원댓글 + 답글 (신고 제외, 신고된 원댓글 밑의 답글 제외)
     * - latestComment: 삭제/신고되지 않은 원댓글 중 최신 1개 (답글 제외)
     * 피드 목록 크기와 무관하게 쿼리 3개(댓글 수 1 + 최신 id 1 + 댓글 로드 1)로 고정된다.
     */
    public Map<Long, CommentAggregate> aggregateByFeedIds(List<Long> feedIds) {
        if (feedIds == null || feedIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> countByFeedId = commentRepository.countVisibleByFeedIds(feedIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        if (countByFeedId.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> latestCommentIdByFeedId = commentRepository.findLatestRootCommentIdsByFeedIds(feedIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Map<Long, Comment> commentById = commentRepository.findAllById(latestCommentIdByFeedId.values()).stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        return countByFeedId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new CommentAggregate(e.getValue(), commentById.get(latestCommentIdByFeedId.get(e.getKey())))
                ));
    }

    /**
     * 사용자 요청에 의한 댓글 1건 삭제 (회원/게스트 공통 삭제 정책)
     * - 원댓글 → 공감, 답글 전체(신고된 답글 포함)와 함께 삭제 ("삭제된 댓글"로 남기지 않음)
     * - 답글 → 해당 답글만 삭제 (답글에는 공감이 없음)
     */
    @Transactional
    public void delete(Comment comment) {
        if (comment.isReply()) {
            commentRepository.delete(comment);
            return;
        }

        // parent_id self FK 때문에 공감 → 답글 → 원댓글 순으로 지운다
        commentLikeRepository.deleteByCommentIdIn(List.of(comment.getId()));
        commentRepository.deleteRepliesByParentIds(List.of(comment.getId()));
        commentRepository.delete(comment);
    }

    @Transactional
    public void report(Comment comment) {
        comment.report();
    }

    @Transactional
    public void deleteByFeed(Feed feed) {
        commentLikeRepository.deleteByCommentFeed(feed);
        commentRepository.deleteRepliesByFeed(feed);
        commentRepository.deleteAllByFeed(feed);
    }

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        if (feeds == null || feeds.isEmpty()) {
            return;
        }
        commentLikeRepository.deleteByCommentFeedIn(feeds);
        commentRepository.deleteRepliesByFeeds(feeds);
        commentRepository.deleteAllByFeeds(feeds);
    }

    /**
     * 회원 탈퇴용. 회원이 누른/받은 공감 → 회원 원댓글에 달린 (남의) 답글 → 회원이 쓴 답글 → 회원 원댓글 순으로 삭제
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        commentLikeRepository.deleteRelatedToUser(userId);
        List<Long> rootIds = commentRepository.findRootCommentIdsByUserId(userId);
        if (!rootIds.isEmpty()) {
            commentRepository.deleteRepliesByParentIds(rootIds);
        }
        commentRepository.deleteRepliesByUserId(userId);
        commentRepository.deleteAllByUserId(userId);
    }
}
