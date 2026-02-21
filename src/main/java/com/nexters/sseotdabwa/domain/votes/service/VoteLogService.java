package com.nexters.sseotdabwa.domain.votes.service;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.entity.VoteLog;
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
}
