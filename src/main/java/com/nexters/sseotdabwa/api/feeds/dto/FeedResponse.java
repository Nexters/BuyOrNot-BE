package com.nexters.sseotdabwa.api.feeds.dto;

import java.time.LocalDateTime;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.entity.FeedImage;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

public record FeedResponse(
        Long feedId,
        String content,
        Long price,
        FeedCategory category,
        Long yesCount,
        Long noCount,
        Long totalCount,
        FeedStatus feedStatus,
        String s3ObjectKey,
        String viewUrl,
        Integer imageWidth,
        Integer imageHeight,
        FeedAuthorResponse author,
        LocalDateTime createdAt,
        Boolean hasVoted,
        VoteChoice myVoteChoice
) {

    public record FeedAuthorResponse(
            Long userId,
            String nickname,
            String profileImage
    ) {}

    public static FeedResponse of(Feed feed, FeedImage feedImage, String viewUrl) {
        return new FeedResponse(
                feed.getId(),
                feed.getContent(),
                feed.getPrice(),
                feed.getCategory(),
                feed.getYesCount(),
                feed.getNoCount(),
                feed.getYesCount() + feed.getNoCount(),
                feed.getFeedStatus(),
                feedImage != null ? feedImage.getS3ObjectKey() : null,
                viewUrl,
                feed.getImageWidth(),
                feed.getImageHeight(),
                new FeedAuthorResponse(
                        feed.getUser().getId(),
                        feed.getUser().getNickname(),
                        feed.getUser().getProfileImage()
                ),
                feed.getCreatedAt(),
                null,
                null
        );
    }

    public static FeedResponse of(Feed feed, FeedImage feedImage, String viewUrl, Boolean hasVoted, VoteChoice myVoteChoice) {
        return new FeedResponse(
                feed.getId(),
                feed.getContent(),
                feed.getPrice(),
                feed.getCategory(),
                feed.getYesCount(),
                feed.getNoCount(),
                feed.getYesCount() + feed.getNoCount(),
                feed.getFeedStatus(),
                feedImage != null ? feedImage.getS3ObjectKey() : null,
                viewUrl,
                feed.getImageWidth(),
                feed.getImageHeight(),
                new FeedAuthorResponse(
                        feed.getUser().getId(),
                        feed.getUser().getNickname(),
                        feed.getUser().getProfileImage()
                ),
                feed.getCreatedAt(),
                hasVoted,
                myVoteChoice
        );
    }
}
