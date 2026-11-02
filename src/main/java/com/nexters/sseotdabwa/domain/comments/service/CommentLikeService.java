package com.nexters.sseotdabwa.domain.comments.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.entity.CommentLike;
import com.nexters.sseotdabwa.domain.comments.repository.CommentLikeRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * 댓글 공감 도메인 서비스
 * - like/unlike는 멱등 (이미 공감한 상태에서 like, 공감 안 한 상태에서 unlike 해도 그대로 성공)
 * - 같은 댓글에 대한 동시 요청은 호출하는 쪽에서 댓글 행을 잠근 상태로 호출해 직렬화한다
 *   (유니크 위반 예외를 잡으면 트랜잭션이 rollback-only가 되어버려서, 예외 대신 잠금으로 중복을 막는다)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public void like(Comment comment, User user) {
        if (commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId())) {
            return;
        }
        commentLikeRepository.save(new CommentLike(comment, user));
    }

    @Transactional
    public void unlike(Comment comment, User user) {
        commentLikeRepository.deleteByCommentIdAndUserId(comment.getId(), user.getId());
    }

    public long countByCommentId(Long commentId) {
        return commentLikeRepository.countByCommentId(commentId);
    }

    public boolean isLiked(Long commentId, User user) {
        return user != null && commentLikeRepository.existsByCommentIdAndUserId(commentId, user.getId());
    }

    /**
     * 댓글 id별 공감 수 (공감 0개인 댓글은 map에 없음)
     */
    public Map<Long, Long> countByCommentIds(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        return commentLikeRepository.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    /**
     * 주어진 댓글들 중 조회자가 공감한 댓글 id 집합 (비로그인이면 빈 집합)
     */
    public Set<Long> findLikedCommentIds(User user, List<Long> commentIds) {
        if (user == null || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(commentLikeRepository.findLikedCommentIds(user.getId(), commentIds));
    }
}
