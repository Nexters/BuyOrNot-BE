package com.nexters.sseotdabwa.domain.feeds.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    List<Feed> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
