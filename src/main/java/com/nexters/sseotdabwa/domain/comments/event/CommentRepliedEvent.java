package com.nexters.sseotdabwa.domain.comments.event;

/**
 * 답글 작성 커밋 후 알림(푸시/이메일) 발송을 트리거하는 이벤트.
 * 커밋 이후 별도 스레드에서 처리되므로 엔티티 대신 id만 담는다.
 */
public record CommentRepliedEvent(Long replyId) {}
