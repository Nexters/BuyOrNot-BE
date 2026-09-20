package com.nexters.sseotdabwa.api.comments.dto;

public record CommentCreateResponse(Long id) {
    public static CommentCreateResponse of(Long id) {
        return new CommentCreateResponse(id);
    }
}
