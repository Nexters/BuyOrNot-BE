package com.nexters.sseotdabwa.api.feeds.dto;

public record FeedCreateResponse(Long feedId) {
    public static FeedCreateResponse of(Long feedId) {
        return new FeedCreateResponse(feedId);
    }
}
