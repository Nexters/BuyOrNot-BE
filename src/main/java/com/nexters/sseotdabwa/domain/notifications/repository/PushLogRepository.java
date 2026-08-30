package com.nexters.sseotdabwa.domain.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexters.sseotdabwa.domain.notifications.entity.PushLog;

public interface PushLogRepository extends JpaRepository<PushLog, Long> {
}
