package com.nexters.sseotdabwa.api.votes.controller;

import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.dto.VoteResponse;
import com.nexters.sseotdabwa.api.votes.facade.VoteFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class VoteController implements VoteControllerSpec {

    private final VoteFacade voteFacade;

    @Override
    @PostMapping("/{feedId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VoteResponse> vote(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody VoteRequest request
    ) {
        VoteResponse response = voteFacade.vote(user, feedId, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{feedId}/votes/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VoteResponse> guestVote(
            @PathVariable Long feedId,
            @Valid @RequestBody VoteRequest request
    ) {
        VoteResponse response = voteFacade.guestVote(feedId, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }
}
