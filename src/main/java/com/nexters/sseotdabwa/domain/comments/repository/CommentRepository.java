package com.nexters.sseotdabwa.domain.comments.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsByFeedIdAndDisplayNickname(Long feedId, String displayNickname);

    @Query("""
        SELECT c FROM Comment c
        WHERE c.feed.id = :feedId
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
          AND (:cursorId IS NULL OR c.id > :cursorId)
        ORDER BY c.id ASC
    """)
    List<Comment> findByFeedIdWithCursorAsc(
            @Param("feedId") Long feedId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
        SELECT c FROM Comment c
        WHERE c.feed.id = :feedId
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
          AND (:cursorId IS NULL OR c.id < :cursorId)
        ORDER BY c.id DESC
    """)
    List<Comment> findByFeedIdWithCursorDesc(
            @Param("feedId") Long feedId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    /**
     * 피드별 (비신고) 댓글 수 + 최신 댓글 id 집계 — 피드 목록의 commentCount/latestComment 배치 조회용
     */
    @Query("""
        SELECT c.feed.id, COUNT(c), MAX(c.id)
        FROM Comment c
        WHERE c.feed.id IN :feedIds
          AND c.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.REPORTED
        GROUP BY c.feed.id
    """)
    List<Object[]> aggregateByFeedIds(@Param("feedIds") List<Long> feedIds);

    void deleteByFeed(Feed feed);

    void deleteByFeedIn(List<Feed> feeds);

    void deleteByUserId(Long userId);
}
