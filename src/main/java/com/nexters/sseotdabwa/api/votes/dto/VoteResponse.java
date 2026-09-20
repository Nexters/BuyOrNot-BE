package com.nexters.sseotdabwa.api.votes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VoteResponse(
        Long feedId,
        VoteChoice choice,
        Long yesCount,
        Long noCount,
        Long totalCount,
        String myProfileImage,
        String voteToken
) {
    public static VoteResponse of(Feed feed, VoteChoice choice, String myProfileImage) {
        return new VoteResponse(
                feed.getId(),
                choice,
                feed.getYesCount(),
                feed.getNoCount(),
                feed.getYesCount() + feed.getNoCount(),
                myProfileImage,
                null
        );
    }

    /**
     * 게스트 투표 응답. voteToken은 이후 댓글(의견) 작성 시 투표 자격 증명에 사용된다.
     */
    public static VoteResponse ofGuest(Feed feed, VoteChoice choice, String voteToken) {
        return new VoteResponse(
                feed.getId(),
                choice,
                feed.getYesCount(),
                feed.getNoCount(),
                feed.getYesCount() + feed.getNoCount(),
                null,
                voteToken
        );
    }
}
