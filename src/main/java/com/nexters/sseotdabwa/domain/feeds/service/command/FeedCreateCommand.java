package com.nexters.sseotdabwa.domain.feeds.service.command;

import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.users.entity.User;

import java.util.List;

public record FeedCreateCommand(
        User user,
        String content,
        Long price,
        FeedCategory category,
        Integer imageWidth,
        Integer imageHeight,
        List<String> s3ObjectKeys,
        String link,
        String title
) {
}
