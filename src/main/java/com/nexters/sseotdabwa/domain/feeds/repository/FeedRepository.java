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

    List<Feed> findByReportStatusNotOrderByCreatedAtDescIdDesc(ReportStatus reportStatus, Pageable pageable);

    List<Feed> findByIdLessThanAndReportStatusNotOrderByCreatedAtDescIdDesc(Long id, ReportStatus reportStatus, Pageable pageable);

    List<Feed> findByFeedStatusAndReportStatusNotOrderByCreatedAtDescIdDesc(FeedStatus feedStatus, ReportStatus reportStatus, Pageable pageable);

    List<Feed> findByIdLessThanAndFeedStatusAndReportStatusNotOrderByCreatedAtDescIdDesc(Long id, FeedStatus feedStatus, ReportStatus reportStatus, Pageable pageable);

    List<Feed> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    List<Feed> findByUserIdAndIdLessThanOrderByCreatedAtDescIdDesc(Long userId, Long id, Pageable pageable);

    List<Feed> findByUserIdAndFeedStatusOrderByCreatedAtDescIdDesc(Long userId, FeedStatus feedStatus, Pageable pageable);

    List<Feed> findByUserIdAndIdLessThanAndFeedStatusOrderByCreatedAtDescIdDesc(Long userId, Long id, FeedStatus feedStatus, Pageable pageable);

    // ===== NOT IN 차단 필터 조합 (Case A~D) =====

    // Case A: 커서 없음, feedStatus 없음
    @Query("""
        SELECT f FROM Feed f
        WHERE f.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.DELETED
          AND f.user.id NOT IN :excludedUserIds
        ORDER BY f.createdAt DESC, f.id DESC
    """)
    List<Feed> findByReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(
            @Param("excludedUserIds") List<Long> excludedUserIds, Pageable pageable);

    // Case B: 커서 있음, feedStatus 없음
    @Query("""
        SELECT f FROM Feed f
        WHERE f.id < :cursor
          AND f.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.DELETED
          AND f.user.id NOT IN :excludedUserIds
        ORDER BY f.createdAt DESC, f.id DESC
    """)
    List<Feed> findByIdLessThanAndReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(
            @Param("cursor") Long cursor,
            @Param("excludedUserIds") List<Long> excludedUserIds, Pageable pageable);

    // Case C: 커서 없음, feedStatus 있음
    @Query("""
        SELECT f FROM Feed f
        WHERE f.feedStatus = :feedStatus
          AND f.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.DELETED
          AND f.user.id NOT IN :excludedUserIds
        ORDER BY f.createdAt DESC, f.id DESC
    """)
    List<Feed> findByFeedStatusAndReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(
            @Param("feedStatus") FeedStatus feedStatus,
            @Param("excludedUserIds") List<Long> excludedUserIds, Pageable pageable);

    // Case D: 커서 있음, feedStatus 있음
    @Query("""
        SELECT f FROM Feed f
        WHERE f.id < :cursor
          AND f.feedStatus = :feedStatus
          AND f.reportStatus <> com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus.DELETED
          AND f.user.id NOT IN :excludedUserIds
        ORDER BY f.createdAt DESC, f.id DESC
    """)
    List<Feed> findByIdLessThanAndFeedStatusAndReportStatusNotAndUserIdNotInOrderByCreatedAtDescIdDesc(
            @Param("cursor") Long cursor,
            @Param("feedStatus") FeedStatus feedStatus,
            @Param("excludedUserIds") List<Long> excludedUserIds, Pageable pageable);

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
