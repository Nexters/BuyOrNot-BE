package com.nexters.sseotdabwa.domain.feeds.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;

import jakarta.persistence.LockModeType;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Feed f WHERE f.id = :feedId")
    Optional<Feed> findByIdWithPessimisticLock(@Param("feedId") Long feedId);

    List<Feed> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    List<Feed> findByReportStatusNotOrderByCreatedAtDesc(ReportStatus reportStatus);

    List<Feed> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ===== 커서 기반 피드 목록 조회 (전체 공개 피드) =====

    @Query("""
        SELECT f FROM Feed f
        WHERE (:cursorId IS NULL OR f.id < :cursorId)
          AND (:feedStatus IS NULL OR f.feedStatus = :feedStatus)
          AND (:category IS NULL OR f.category = :category)
          AND f.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.DELETED
        ORDER BY f.id DESC
    """)
    List<Feed> findFeedsWithCursor(
            @Param("cursorId") Long cursorId,
            @Param("feedStatus") FeedStatus feedStatus,
            @Param("category") FeedCategory category,
            Pageable pageable);

    // ===== 커서 기반 피드 목록 조회 (특정 유저 제외) =====

    @Query("""
        SELECT f FROM Feed f
        WHERE (:cursorId IS NULL OR f.id < :cursorId)
          AND (:feedStatus IS NULL OR f.feedStatus = :feedStatus)
          AND (:category IS NULL OR f.category = :category)
          AND f.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.DELETED
          AND f.user.id NOT IN :excludedUserIds
        ORDER BY f.id DESC
    """)
    List<Feed> findFeedsWithCursorExcludingUsers(
            @Param("cursorId") Long cursorId,
            @Param("feedStatus") FeedStatus feedStatus,
            @Param("category") FeedCategory category,
            @Param("excludedUserIds") List<Long> excludedUserIds,
            Pageable pageable);

    // ===== 내 피드 커서 기반 조회 =====

    @Query("""
        SELECT f FROM Feed f
        WHERE f.user.id = :userId
          AND (:cursorId IS NULL OR f.id < :cursorId)
          AND (:feedStatus IS NULL OR f.feedStatus = :feedStatus)
          AND (:category IS NULL OR f.category = :category)
        ORDER BY f.id DESC
    """)
    List<Feed> findMyFeedsWithCursor(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            @Param("feedStatus") FeedStatus feedStatus,
            @Param("category") FeedCategory category,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Feed f SET f.feedStatus = 'CLOSED', f.updatedAt = :now WHERE f.feedStatus = 'OPEN' AND f.createdAt < :cutoff")
    int closeExpiredFeeds(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);

    /**
     * 마감 대상 feedId 조회
     * - OPEN
     * - createdAt <= cutoff
     * - reportStatus NOT IN (DELETED, REPORTED)
     */
    @Query("""
        select f.id
        from Feed f
        where f.feedStatus = com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus.OPEN
          and f.createdAt <= :cutoff
          and f.reportStatus not in :excludedReportStatuses
    """)
    List<Long> findExpiredOpenFeedIds(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("excludedReportStatuses") List<ReportStatus> excludedReportStatuses
    );

    /**
     * feedId 대상만 bulk CLOSED 처리
     */
    @Modifying
    @Query("""
        update Feed f
        set f.feedStatus = com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus.CLOSED,
            f.updatedAt = :now
        where f.id in :feedIds
          and f.feedStatus = com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus.OPEN
    """)
    int closeFeedsByIds(@Param("feedIds") List<Long> feedIds, @Param("now") LocalDateTime now);
}
