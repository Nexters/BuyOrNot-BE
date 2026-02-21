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

    List<Feed> findByReportStatusNotOrderByIdDesc(ReportStatus reportStatus, Pageable pageable);

    List<Feed> findByIdLessThanAndReportStatusNotOrderByIdDesc(Long id, ReportStatus reportStatus, Pageable pageable);

    @Modifying
    @Query("UPDATE Feed f SET f.feedStatus = 'CLOSED', f.updatedAt = :now WHERE f.feedStatus = 'OPEN' AND f.createdAt < :cutoff")
    int closeExpiredFeeds(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);
}
