package com.nexters.sseotdabwa.domain.votes.repository;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VoteLogRepository extends JpaRepository<VoteLog, Long> {

    void deleteByUserId(Long userId);

    void deleteByFeedIn(List<Feed> feeds);

    void deleteByFeed(Feed feed);

    boolean existsByUserIdAndFeedId(Long userId, Long feedId);

    List<VoteLog> findByUserIdAndFeedIdIn(Long userId, List<Long> feedIds);

    /**
     * 특정 피드에 투표한 userId 목록 (guest 제외)
     * - VoteType.USER
     * - user is not null
     * - distinct
     */
    @Query("""
        select distinct v.user.id
        from VoteLog v
        where v.feed.id = :feedId
          and v.voteType = com.nexters.sseotdabwa.domain.votes.enums.VoteType.USER
          and v.user is not null
    """)
    List<Long> findDistinctUserIdsVotedByFeedId(Long feedId);
}
