package com.nexters.sseotdabwa.domain.votes.event;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;

public record VoteCreatedEvent(Feed feed) {}
