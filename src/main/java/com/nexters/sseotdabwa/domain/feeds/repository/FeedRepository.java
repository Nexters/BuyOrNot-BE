package com.nexters.sseotdabwa.domain.feeds.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    List<Feed> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    List<Feed> findByReportStatusNotOrderByCreatedAtDesc(ReportStatus reportStatus);

    List<Feed> findByUserIdOrderByCreatedAtDesc(Long userId);
}
