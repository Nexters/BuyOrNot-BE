package com.nexters.sseotdabwa.api.comments.controller;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.comments.facade.CommentFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class CommentController implements CommentControllerSpec {

    private final CommentFacade commentFacade;

    @Override
    @PostMapping("/{feedId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentCreateResponse> createComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentCreateResponse response = commentFacade.createComment(user, feedId, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{feedId}/comments/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentCreateResponse> createGuestComment(
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequestGuest request
    ) {
        CommentCreateResponse response = commentFacade.createGuestComment(feedId, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/{feedId}/comments")
    public ApiResponse<CursorPageResponse<CommentResponse>> getComments(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) CommentSort sort
    ) {
        CursorPageResponse<CommentResponse> response = commentFacade.getComments(user, feedId, cursor, size, sort);
        return ApiResponse.success(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{feedId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId
    ) {
        commentFacade.deleteComment(user, commentId);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{feedId}/comments/{commentId}/guest")
    public ApiResponse<Void> deleteGuestComment(
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentGuestDeleteRequest request
    ) {
        commentFacade.deleteGuestComment(commentId, request);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Override
    @PostMapping("/{feedId}/comments/{commentId}/report")
    public ApiResponse<Void> reportComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId
    ) {
        commentFacade.reportComment(user, commentId);
        return ApiResponse.success(HttpStatus.OK);
    }
}
