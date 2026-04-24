package com.nexters.sseotdabwa.domain.feeds.repository;

import java.util.List;
import java.util.Optional;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    void deleteByFeedIn(List<Feed> feeds);

    List<FeedImage> findByFeedInOrderByIdAsc(List<Feed> feeds);

    List<FeedImage> findByFeedOrderByIdAsc(Feed feed);

    void deleteByFeed(Feed feed);

    @Query("""
        select fi
        from FeedImage fi
        join fetch fi.feed f
        where f.id in :feedIds
        order by fi.id asc
    """)
    List<FeedImage> findByFeedIds(@Param("feedIds") List<Long> feedIds);
}
