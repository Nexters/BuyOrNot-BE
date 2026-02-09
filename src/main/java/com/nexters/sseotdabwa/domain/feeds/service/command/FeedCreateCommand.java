package com.nexters.sseotdabwa.domain.feeds.service.command;

import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.users.entity.User;

public record FeedCreateCommand(
        User user,
        String content,
        Long price,
        FeedCategory category,
        Integer imageWidth,
        Integer imageHeight,
        String s3ObjectKey
) {
}
