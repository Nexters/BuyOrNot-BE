package com.nexters.sseotdabwa.domain.feeds.repository;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedReviewRepository extends JpaRepository<FeedReview, Long> {

    void deleteByFeedIn(List<Feed> feeds);
}
