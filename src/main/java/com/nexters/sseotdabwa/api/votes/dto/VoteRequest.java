package com.nexters.sseotdabwa.api.votes.dto;

import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull(message = "투표 선택은 필수입니다.")
        VoteChoice choice
) {}
