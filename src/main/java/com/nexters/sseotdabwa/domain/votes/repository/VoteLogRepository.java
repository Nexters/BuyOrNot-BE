package com.nexters.sseotdabwa.domain.votes.repository;

import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteLogRepository extends JpaRepository<VoteLog, Long> {
}
