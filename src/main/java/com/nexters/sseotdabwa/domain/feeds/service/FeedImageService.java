package com.nexters.sseotdabwa.domain.feeds.service;

import com.nexters.sseotdabwa.domain.feeds.repository.FeedImageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedImageService {

    private final FeedImageRepository feedImageRepository;
}
