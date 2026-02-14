package com.nexters.sseotdabwa.domain.feeds.repository;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    void deleteByFeedIn(List<Feed> feeds);

    List<FeedImage> findByFeedIn(List<Feed> feeds);

    void deleteByFeed(Feed feed);
}
