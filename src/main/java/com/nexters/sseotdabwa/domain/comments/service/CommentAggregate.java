package com.nexters.sseotdabwa.domain.comments.service;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;

/**
 * 피드 하나에 대한 (비신고) 댓글 수 + 가장 최근 댓글 1개
 */
public record CommentAggregate(Long commentCount, Comment latestComment) {}
