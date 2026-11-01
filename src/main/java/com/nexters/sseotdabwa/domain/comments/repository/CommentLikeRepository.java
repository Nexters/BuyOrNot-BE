package com.nexters.sseotdabwa.domain.comments.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.comments.entity.CommentLike;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommentLike l WHERE l.comment.id = :commentId AND l.user.id = :userId")
    int deleteByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 댓글별 공감 수 — 댓글 목록 배치 조회용 (row: [commentId, count])
     */
    @Query("""
        SELECT l.comment.id, COUNT(l)
        FROM CommentLike l
        WHERE l.comment.id IN :commentIds
        GROUP BY l.comment.id
    """)
    List<Object[]> countByCommentIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 주어진 댓글들 중 해당 회원이 공감한 댓글 id 목록
     */
    @Query("SELECT l.comment.id FROM CommentLike l WHERE l.user.id = :userId AND l.comment.id IN :commentIds")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommentLike l WHERE l.comment.id IN :commentIds")
    void deleteByCommentIdIn(@Param("commentIds") List<Long> commentIds);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommentLike l WHERE l.comment.id IN (SELECT c.id FROM Comment c WHERE c.feed = :feed)")
    void deleteByCommentFeed(@Param("feed") Feed feed);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CommentLike l WHERE l.comment.id IN (SELECT c.id FROM Comment c WHERE c.feed IN :feeds)")
    void deleteByCommentFeedIn(@Param("feeds") List<Feed> feeds);

    /**
     * 회원이 누른 공감 + 회원이 쓴 댓글에 달린 공감을 모두 삭제 (회원 탈퇴용)
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM CommentLike l
        WHERE l.user.id = :userId
           OR l.comment.id IN (SELECT c.id FROM Comment c WHERE c.user.id = :userId)
    """)
    void deleteRelatedToUser(@Param("userId") Long userId);
}
