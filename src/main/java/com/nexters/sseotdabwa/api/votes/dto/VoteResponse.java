package com.nexters.sseotdabwa.api.votes.dto;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

public record VoteResponse(
        Long feedId,
        VoteChoice choice,
        Long yesCount,
        Long noCount
) {
    public static VoteResponse of(Feed feed, VoteChoice choice) {
        return new VoteResponse(
                feed.getId(),
                choice,
                feed.getYesCount(),
                feed.getNoCount()
        );
    }
}
