package com.nexters.sseotdabwa.domain.feeds.service.command;

public record FeedImageCreateInfo(
        String s3ObjectKey,
        Integer imageWidth,
        Integer imageHeight
) {
}
