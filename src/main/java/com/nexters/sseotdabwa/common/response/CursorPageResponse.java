package com.nexters.sseotdabwa.common.response;

import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    Long nextCursor,
    boolean hasNext
) {
    public static <T> CursorPageResponse<T> of(List<T> content, Long nextCursor, boolean hasNext) {
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
