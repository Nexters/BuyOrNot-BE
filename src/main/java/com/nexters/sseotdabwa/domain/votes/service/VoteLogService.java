package com.nexters.sseotdabwa.domain.votes.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;
import com.nexters.sseotdabwa.domain.votes.service.command.VoteCreateCommand;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteLogService {

    private final VoteLogRepository voteLogRepository;

    @Transactional
    public void deleteByUserId(Long userId) {
        voteLogRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        voteLogRepository.deleteByFeedIn(feeds);
    }

    @Transactional
    public void deleteByFeed(Feed feed) {
        voteLogRepository.deleteByFeed(feed);
    }

    @Transactional
    public VoteLog createVoteLog(VoteCreateCommand command) {
        VoteLog voteLog = VoteLog.builder()
                .user(command.user())
                .feed(command.feed())
                .choice(command.choice())
                .voteType(command.voteType())
                .build();
        return voteLogRepository.save(voteLog);
    }

    public boolean existsByUserAndFeed(Long userId, Long feedId) {
        return voteLogRepository.existsByUserIdAndFeedId(userId, feedId);
    }

    public List<VoteLog> findByUserIdAndFeedIds(Long userId, List<Long> feedIds) {
        return voteLogRepository.findByUserIdAndFeedIdIn(userId, feedIds);
    }

    public List<Long> findDistinctUserIdsVotedByFeedId(Long feedId) {
        return voteLogRepository.findDistinctUserIdsVotedByFeedId(feedId);
    }

    /**
     * 한 피드에 대한 여러 작성자의 투표 선택 — 댓글 목록의 isAuthor/voteChoice 계산용
     */
    public Map<Long, VoteChoice> findChoicesByFeedIdAndUserIds(Long feedId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return voteLogRepository.findByFeedIdAndUserIdIn(feedId, userIds).stream()
                .collect(Collectors.toMap(vl -> vl.getUser().getId(), VoteLog::getChoice));
    }

    /**
     * 여러 피드 x 여러 작성자의 투표 선택 — 피드 목록 댓글 프리뷰(latestComment)의 voteChoice 계산용
     */
    public Map<FeedUserKey, VoteChoice> findChoicesByFeedIdsAndUserIds(List<Long> feedIds, List<Long> userIds) {
        if (feedIds == null || feedIds.isEmpty() || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return voteLogRepository.findByFeedIdInAndUserIdIn(feedIds, userIds).stream()
                .collect(Collectors.toMap(
                        vl -> new FeedUserKey(vl.getFeed().getId(), vl.getUser().getId()),
                        VoteLog::getChoice
                ));
    }
}
