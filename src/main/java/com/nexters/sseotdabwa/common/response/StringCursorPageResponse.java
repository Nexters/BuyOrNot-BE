package com.nexters.sseotdabwa.common.response;

import java.util.List;

/**
 * 문자열 커서 페이지 응답
 * - 단일 Long id로 표현할 수 없는 복합 커서(예: 핫플 "{투표수}_{feedId}")용. 형태는 CursorPageResponse와 동일.
 */
public record StringCursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    boolean hasNext
) {
    public static <T> StringCursorPageResponse<T> of(List<T> content, String nextCursor, boolean hasNext) {
        return new StringCursorPageResponse<>(content, nextCursor, hasNext);
    }
}
