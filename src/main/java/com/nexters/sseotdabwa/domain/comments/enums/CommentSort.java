package com.nexters.sseotdabwa.domain.comments.enums;

/**
 * 댓글 목록 정렬 기준
 * - REGISTERED: 등록순(오래된 순, 기본값)
 * - LATEST: 최신순
 * - 향후 인기순/추천순 등 값 추가로 확장 가능
 */
public enum CommentSort {
    REGISTERED,
    LATEST
}
