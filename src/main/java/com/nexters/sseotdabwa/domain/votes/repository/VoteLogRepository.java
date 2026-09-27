package com.nexters.sseotdabwa.domain.votes.repository;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteLogRepository extends JpaRepository<VoteLog, Long> {

    void deleteByUserId(Long userId);

    void deleteByFeedIn(List<Feed> feeds);

    void deleteByFeed(Feed feed);

    boolean existsByUserIdAndFeedId(Long userId, Long feedId);

    List<VoteLog> findByUserIdAndFeedIdIn(Long userId, List<Long> feedIds);

    /**
     * 댓글 목록의 isAuthor/voteChoice 계산용 — 한 피드에 대한 여러 작성자의 투표 여부를 한 번에 조회
     */
    List<VoteLog> findByFeedIdAndUserIdIn(Long feedId, List<Long> userIds);

    /**
     * 피드 목록의 댓글 프리뷰(latestComment) isAuthor/voteChoice 계산용 — 여러 피드 x 여러 작성자 조합을 한 번에 조회.
     * 정확한 (feedId, userId) 페어가 아니라 각 IN 조건의 교집합이지만, 두 목록 모두 페이지 크기 규모라 결과가 작다.
     */
    List<VoteLog> findByFeedIdInAndUserIdIn(List<Long> feedIds, List<Long> userIds);

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
    List<Long> findDistinctUserIdsVotedByFeedId(@Param("feedId") Long feedId);
}
