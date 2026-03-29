package com.nexters.sseotdabwa.api.feeds.dto;

import java.time.LocalDateTime;
import java.util.List;

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
        List<String> s3ObjectKeys,
        List<String> imageUrls,
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

    public static FeedResponse of(Feed feed, List<FeedImage> images, List<String> viewUrls) {
        return new FeedResponse(
                feed.getId(),
                feed.getContent(),
                feed.getPrice(),
                feed.getCategory(),
                feed.getYesCount(),
                feed.getNoCount(),
                feed.getYesCount() + feed.getNoCount(),
                feed.getFeedStatus(),
                images.stream().map(FeedImage::getS3ObjectKey).toList(),
                viewUrls,
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

    public static FeedResponse of(Feed feed, List<FeedImage> images, List<String> viewUrls, Boolean hasVoted, VoteChoice myVoteChoice) {
        return new FeedResponse(
                feed.getId(),
                feed.getContent(),
                feed.getPrice(),
                feed.getCategory(),
                feed.getYesCount(),
                feed.getNoCount(),
                feed.getYesCount() + feed.getNoCount(),
                feed.getFeedStatus(),
                images.stream().map(FeedImage::getS3ObjectKey).toList(),
                viewUrls,
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
