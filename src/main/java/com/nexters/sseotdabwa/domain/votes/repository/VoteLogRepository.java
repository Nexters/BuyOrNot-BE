package com.nexters.sseotdabwa.domain.votes.repository;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteLogRepository extends JpaRepository<VoteLog, Long> {

    void deleteByUserId(Long userId);

    void deleteByFeedIn(List<Feed> feeds);

    void deleteByFeed(Feed feed);
}
