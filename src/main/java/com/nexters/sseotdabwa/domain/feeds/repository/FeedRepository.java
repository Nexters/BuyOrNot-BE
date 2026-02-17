package com.nexters.sseotdabwa.domain.feeds.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
