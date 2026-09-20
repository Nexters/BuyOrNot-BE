package com.nexters.sseotdabwa.api.comments.dto;

import java.time.LocalDateTime;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.users.entity.User;

public record CommentResponse(
        Long id,
        String authorType,
        String nickname,
        String profileImage,
        String content,
        boolean isMine,
        LocalDateTime createdAt
) {
    public static CommentResponse of(Comment comment, User currentUser, String profileImage) {
        boolean isMine = !comment.isGuestComment()
                && currentUser != null
                && comment.getUser().getId().equals(currentUser.getId());

        return new CommentResponse(
                comment.getId(),
                comment.isGuestComment() ? "GUEST" : "MEMBER",
                comment.getDisplayNickname(),
                profileImage,
                comment.getContent(),
                isMine,
                comment.getCreatedAt()
        );
    }
}
