package com.nexters.sseotdabwa.domain.votes.service;

/**
 * (feedId, userId) 조합 키 — 여러 피드 x 여러 작성자의 투표 선택을 배치 조회할 때 맵 키로 사용
 */
public record FeedUserKey(Long feedId, Long userId) {}
