package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedReviewRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReviewService {

    private final FeedReviewRepository feedReviewRepository;

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        feedReviewRepository.deleteByFeedIn(feeds);
    }
}
