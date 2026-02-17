package com.nexters.sseotdabwa.domain.votes.service.command;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;

public record VoteCreateCommand(
        User user,
        Feed feed,
        VoteChoice choice,
        VoteType voteType
) {}
