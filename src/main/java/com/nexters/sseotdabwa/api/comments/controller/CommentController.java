package com.nexters.sseotdabwa.api.comments.controller;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentLikeResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentReplyResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentThreadResponse;
import com.nexters.sseotdabwa.api.comments.facade.CommentFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.common.security.CurrentUser;
import com.nexters.sseotdabwa.common.util.ClientIpResolver;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.servlet.http.HttpServletRequest;
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
            @Valid @RequestBody CommentCreateRequestGuest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest
    ) {
        String ip = ClientIpResolver.resolve(httpRequest);
        CommentCreateResponse response = commentFacade.createGuestComment(feedId, request, ip, deviceId);
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

    @Override
    @PostMapping("/{feedId}/comments/{commentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentCreateResponse> createReply(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentCreateResponse response = commentFacade.createReply(user, feedId, commentId, request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @PostMapping("/{feedId}/comments/{commentId}/replies/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentCreateResponse> createGuestReply(
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentCreateRequestGuest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest
    ) {
        String ip = ClientIpResolver.resolve(httpRequest);
        CommentCreateResponse response = commentFacade.createGuestReply(feedId, commentId, request, ip, deviceId);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/{feedId}/comments/{commentId}")
    public ApiResponse<CommentThreadResponse> getCommentThread(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentFacade.getCommentThread(user, feedId, commentId), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{feedId}/comments/{commentId}/replies")
    public ApiResponse<CursorPageResponse<CommentReplyResponse>> getReplies(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(commentFacade.getReplies(user, feedId, commentId, cursor, size), HttpStatus.OK);
    }

    @Override
    @PostMapping("/{feedId}/comments/{commentId}/likes")
    public ApiResponse<CommentLikeResponse> likeComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentFacade.likeComment(user, feedId, commentId), HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/{feedId}/comments/{commentId}/likes")
    public ApiResponse<CommentLikeResponse> unlikeComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentFacade.unlikeComment(user, feedId, commentId), HttpStatus.OK);
    }
}
