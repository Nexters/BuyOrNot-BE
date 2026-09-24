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
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
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
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 300;
    private static final int MAX_NICKNAME_RETRY = 5;

    private final CommentRepository commentRepository;
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

    @Transactional
    public Comment createGuestComment(Feed feed, String requestedNickname, String guestPasswordHash, String guestProfileImage, String content) {
        String normalized = validateAndNormalizeContent(content);
        String nickname = resolveUniqueGuestNickname(feed.getId(), requestedNickname);

        Comment comment = Comment.builder()
                .feed(feed)
                .user(null)
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
     * 피드 내 댓글 목록 (커서 기반 페이지네이션, 등록순/최신순)
     */
    public List<Comment> findByFeedIdWithCursor(Long feedId, Long cursor, int size, CommentSort sort) {
        var pageable = PageRequest.ofSize(size + 1);
        return sort == CommentSort.LATEST
                ? commentRepository.findByFeedIdWithCursorDesc(feedId, cursor, pageable)
                : commentRepository.findByFeedIdWithCursorAsc(feedId, cursor, pageable);
    }

    /**
     * feedId별 (비신고 댓글 수 + 최신 댓글 1개) 집계 — 피드 목록 응답 확장(commentCount/latestComment)용.
     * 피드 목록 크기와 무관하게 쿼리 2개(집계 1 + 댓글 로드 1)로 고정된다.
     */
    public Map<Long, CommentAggregate> aggregateByFeedIds(List<Long> feedIds) {
        if (feedIds == null || feedIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows = commentRepository.aggregateByFeedIds(feedIds);
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> latestCommentIdByFeedId = rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[2]));
        Map<Long, Comment> commentById = commentRepository.findAllById(latestCommentIdByFeedId.values()).stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new CommentAggregate((Long) row[1], commentById.get(latestCommentIdByFeedId.get((Long) row[0])))
                ));
    }

    @Transactional
    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }

    @Transactional
    public void report(Comment comment) {
        comment.report();
    }

    @Transactional
    public void deleteByFeed(Feed feed) {
        commentRepository.deleteByFeed(feed);
    }

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        commentRepository.deleteByFeedIn(feeds);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        commentRepository.deleteByUserId(userId);
    }
}
