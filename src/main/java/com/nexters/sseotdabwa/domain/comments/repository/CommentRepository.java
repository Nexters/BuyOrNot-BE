package com.nexters.sseotdabwa.domain.comments.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;

import jakarta.persistence.LockModeType;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsByFeedIdAndDisplayNickname(Long feedId, String displayNickname);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comment c WHERE c.id = :commentId")
    Optional<Comment> findByIdWithLock(@Param("commentId") Long commentId);

    /**
     * 원댓글 목록 (등록순). 답글과 신고된 원댓글은 제외한다.
     */
    @Query("""
        SELECT c FROM Comment c
        WHERE c.feed.id = :feedId
          AND c.parent IS NULL
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
          AND (:cursorId IS NULL OR c.id > :cursorId)
        ORDER BY c.id ASC
    """)
    List<Comment> findByFeedIdWithCursorAsc(
            @Param("feedId") Long feedId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    /**
     * 원댓글 목록 (최신순). 조건은 등록순과 동일.
     */
    @Query("""
        SELECT c FROM Comment c
        WHERE c.feed.id = :feedId
          AND c.parent IS NULL
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
          AND (:cursorId IS NULL OR c.id < :cursorId)
        ORDER BY c.id DESC
    """)
    List<Comment> findByFeedIdWithCursorDesc(
            @Param("feedId") Long feedId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    /**
     * 원댓글들의 (비신고) 답글 전체 — 댓글 목록에 답글을 붙이기 위한 배치 조회
     */
    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.user
        WHERE c.parent.id IN :parentIds
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
        ORDER BY c.parent.id ASC, c.id ASC
    """)
    List<Comment> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    /**
     * 답글 페이지용 답글 목록 (등록순 고정, 커서 기반)
     */
    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.user
        WHERE c.parent.id = :parentId
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
          AND (:cursorId IS NULL OR c.id > :cursorId)
        ORDER BY c.id ASC
    """)
    List<Comment> findRepliesWithCursor(
            @Param("parentId") Long parentId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    long countByParentIdAndReportStatusNot(Long parentId, ReportStatus reportStatus);

    /**
     * 답글 id → 원댓글 id (row: [replyId, parentId]) — 답글 알림 목록에서 답글 페이지로 이동할 원댓글 id를 찾을 때 사용
     */
    @Query("SELECT c.id, c.parent.id FROM Comment c WHERE c.id IN :replyIds AND c.parent IS NOT NULL")
    List<Object[]> findParentIdsByReplyIds(@Param("replyIds") List<Long> replyIds);

    /**
     * 스레드에 답글을 단 회원 id 목록 (답글 알림 수신자 산정용, 게스트 답글은 user가 없어 제외됨)
     */
    @Query("SELECT DISTINCT c.user.id FROM Comment c WHERE c.parent.id = :parentId AND c.user IS NOT NULL")
    List<Long> findDistinctReplyAuthorIds(@Param("parentId") Long parentId);

    /**
     * 피드별 댓글 수 — 원댓글 + 답글 (row: [feedId, count])
     * - 제외: 신고된 댓글/답글, 신고된 원댓글 밑의 답글(목록에 보이지 않으므로)
     */
    @Query("""
        SELECT c.feed.id, COUNT(c)
        FROM Comment c
        LEFT JOIN c.parent p
        WHERE c.feed.id IN :feedIds
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
          AND (p IS NULL OR p.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED)
        GROUP BY c.feed.id
    """)
    List<Object[]> countVisibleByFeedIds(@Param("feedIds") List<Long> feedIds);

    /**
     * 피드별 가장 최근 원댓글 id — 피드 목록의 latestComment 미리보기용 (row: [feedId, commentId])
     * - 답글은 맥락 없이 미리보기에 뜨지 않도록 제외, 신고된 원댓글도 제외
     */
    @Query("""
        SELECT c.feed.id, MAX(c.id)
        FROM Comment c
        WHERE c.feed.id IN :feedIds
          AND c.parent IS NULL
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
        GROUP BY c.feed.id
    """)
    List<Object[]> findLatestRootCommentIdsByFeedIds(@Param("feedIds") List<Long> feedIds);

    // ===== 삭제 =====
    // parent_id self FK 때문에 항상 답글 → 원댓글 순으로 지운다.
    // (같은 테이블을 서브쿼리로 참조하는 DELETE는 MySQL에서 1093 에러가 나므로, 원댓글 id는 먼저 조회해서 넘긴다)

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.parent.id IN :parentIds")
    void deleteRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.feed = :feed AND c.parent IS NOT NULL")
    void deleteRepliesByFeed(@Param("feed") Feed feed);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.feed = :feed")
    void deleteAllByFeed(@Param("feed") Feed feed);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.feed IN :feeds AND c.parent IS NOT NULL")
    void deleteRepliesByFeeds(@Param("feeds") List<Feed> feeds);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.feed IN :feeds")
    void deleteAllByFeeds(@Param("feeds") List<Feed> feeds);

    @Query("SELECT c.id FROM Comment c WHERE c.user.id = :userId AND c.parent IS NULL")
    List<Long> findRootCommentIdsByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.user.id = :userId AND c.parent IS NOT NULL")
    void deleteRepliesByUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
