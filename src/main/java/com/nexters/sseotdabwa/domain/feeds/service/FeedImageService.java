package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;
import java.util.Optional;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedImageService {

    private final FeedImageRepository feedImageRepository;

    /**
     * Feed : FeedImage = 1 : 1 저장
     */
    @Transactional
    public void saveAll(Feed feed, List<String> s3ObjectKeys) {
        List<FeedImage> images = s3ObjectKeys.stream()
                .map(key -> FeedImage.builder()
                        .feed(feed)
                        .s3ObjectKey(key.trim())
                        .build())
                .toList();

        feedImageRepository.saveAll(images);
    }

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        feedImageRepository.deleteByFeedIn(feeds);
    }

    public List<FeedImage> findByFeed(Feed feed) {
        return feedImageRepository.findByFeed(feed);
    }

    public List<FeedImage> findByFeeds(List<Feed> feeds) {
        return feedImageRepository.findByFeedIn(feeds);
    }

    @Transactional
    public void deleteByFeed(Feed feed) {
        feedImageRepository.deleteByFeed(feed);
    }

    public List<FeedImage> findByFeedIds(List<Long> feedIds) {
        return feedImageRepository.findByFeedIds(feedIds);
    }
}
