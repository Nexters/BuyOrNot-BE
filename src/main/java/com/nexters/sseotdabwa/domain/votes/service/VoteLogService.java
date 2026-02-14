package com.nexters.sseotdabwa.domain.votes.service;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

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
}
