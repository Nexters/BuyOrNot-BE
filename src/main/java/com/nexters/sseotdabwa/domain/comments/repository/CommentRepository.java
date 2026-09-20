package com.nexters.sseotdabwa.domain.comments.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    boolean existsByFeedIdAndDisplayNickname(Long feedId, String displayNickname);

    @Query("""
        SELECT c FROM Comment c
        WHERE c.feed.id = :feedId
          AND (:cursorId IS NULL OR c.id > :cursorId)
        ORDER BY c.id ASC
    """)
    List<Comment> findByFeedIdWithCursor(
            @Param("feedId") Long feedId,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    void deleteByFeed(Feed feed);

    void deleteByFeedIn(List<Feed> feeds);

    void deleteByUserId(Long userId);
}
