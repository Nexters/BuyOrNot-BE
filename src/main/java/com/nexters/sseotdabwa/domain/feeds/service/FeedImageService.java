package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedImageService {

    private final FeedImageRepository feedImageRepository;

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        feedImageRepository.deleteByFeedIn(feeds);
    }
}
