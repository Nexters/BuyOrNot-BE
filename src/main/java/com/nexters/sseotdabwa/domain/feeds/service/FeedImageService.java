package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;
import java.util.Optional;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;
import com.nexters.sseotdabwa.domain.feeds.service.command.FeedImageCreateInfo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedImageService {

    private final FeedImageRepository feedImageRepository;

    @Transactional
    public void saveAll(Feed feed, List<FeedImageCreateInfo> imageInfos) {
        List<FeedImage> images = imageInfos.stream()
                .map(info -> FeedImage.builder()
                        .feed(feed)
                        .s3ObjectKey(info.s3ObjectKey().trim())
                        .imageWidth(info.imageWidth())
                        .imageHeight(info.imageHeight())
                        .build())
                .toList();

        feedImageRepository.saveAll(images);
    }

    @Transactional
    public void deleteByFeeds(List<Feed> feeds) {
        feedImageRepository.deleteByFeedIn(feeds);
    }

    public List<FeedImage> findByFeed(Feed feed) {
        return feedImageRepository.findByFeedOrderByIdAsc(feed);
    }

    public List<FeedImage> findByFeeds(List<Feed> feeds) {
        return feedImageRepository.findByFeedInOrderByIdAsc(feeds);
    }

    @Transactional
    public void deleteByFeed(Feed feed) {
        feedImageRepository.deleteByFeed(feed);
    }

    public List<FeedImage> findByFeedIds(List<Long> feedIds) {
        return feedImageRepository.findByFeedIds(feedIds);
    }
}
