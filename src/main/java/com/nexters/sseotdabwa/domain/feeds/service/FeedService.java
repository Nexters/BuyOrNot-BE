package com.nexters.sseotdabwa.domain.feeds.service;

import java.util.List;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;

    public List<Feed> findByUserId(Long userId) {
        return feedRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        feedRepository.deleteByUserId(userId);
    }
}
