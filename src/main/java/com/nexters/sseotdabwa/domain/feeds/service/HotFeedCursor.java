package com.nexters.sseotdabwa.domain.feeds.service;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.exception.FeedErrorCode;

/**
 * 핫플 투표 목록 복합 커서 (투표 수, feedId)
 * - 문자열 표현: "{투표수}_{feedId}" (예: "12_345")
 * - 투표 수만으로는 동률이 많아 feedId를 함께 써야 페이지 경계에서 누락/중복이 생기지 않는다.
 */
public record HotFeedCursor(long votes, long feedId) {

    private static final String DELIMITER = "_";

    public static HotFeedCursor from(Feed feed) {
        return new HotFeedCursor(feed.getYesCount() + feed.getNoCount(), feed.getId());
    }

    /**
     * null/공백이면 첫 페이지(null) 반환, 형식이 잘못되면 400(FEED_INVALID_CURSOR)
     */
    public static HotFeedCursor parse(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = cursor.split(DELIMITER, -1);
        if (parts.length != 2) {
            throw new GlobalException(FeedErrorCode.FEED_INVALID_CURSOR);
        }
        try {
            long votes = Long.parseLong(parts[0]);
            long feedId = Long.parseLong(parts[1]);
            if (votes < 0 || feedId < 1) {
                throw new GlobalException(FeedErrorCode.FEED_INVALID_CURSOR);
            }
            return new HotFeedCursor(votes, feedId);
        } catch (NumberFormatException e) {
            throw new GlobalException(FeedErrorCode.FEED_INVALID_CURSOR);
        }
    }

    public String encode() {
        return votes + DELIMITER + feedId;
    }
}
