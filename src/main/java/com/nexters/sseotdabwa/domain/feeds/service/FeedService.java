package com.nexters.sseotdabwa.domain.feeds.service;

import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
}
