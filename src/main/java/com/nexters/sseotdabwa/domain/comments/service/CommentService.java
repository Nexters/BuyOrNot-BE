package com.nexters.sseotdabwa.domain.comments.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
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
 * - content: 100자 이하 (Feed.content와 동일 기준)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 100;
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
    public Comment createGuestComment(Feed feed, String requestedNickname, String guestProfileImage, String content) {
        String normalized = validateAndNormalizeContent(content);
        String nickname = resolveUniqueGuestNickname(feed.getId(), requestedNickname);

        Comment comment = Comment.builder()
                .feed(feed)
                .user(null)
                .guestNickname(nickname)
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

    /**
     * 피드 내 댓글 목록 (등록순, 커서 기반 페이지네이션)
     */
    public List<Comment> findByFeedIdWithCursor(Long feedId, Long cursor, int size) {
        return commentRepository.findByFeedIdWithCursor(feedId, cursor, PageRequest.ofSize(size + 1));
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
