package com.nexters.sseotdabwa.api.comments.dto;

import java.time.LocalDateTime;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.users.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 답글 응답. 원댓글(CommentResponse)과 달리 투표 태그/버블(voteChoice), 공감, 답글 관련 필드가 없다
 * — 투표 내용은 투표 페이지에서 남긴 원댓글에만 노출하고, 답글에는 답글/공감을 달 수 없기 때문.
 */
@Schema(description = "답글 응답")
public record CommentReplyResponse(
        @Schema(description = "답글 id")
        Long id,
        @Schema(description = "원댓글 id")
        Long parentId,
        @Schema(description = "작성자 유형. MEMBER(회원) 또는 GUEST(게스트)")
        String authorType,
        @Schema(description = "작성 시점 닉네임 스냅샷")
        String nickname,
        @Schema(description = "작성자 프로필 이미지 URL")
        String profileImage,
        @Schema(description = "답글 내용")
        String content,
        @Schema(description = "지금 조회 중인 회원 본인이 작성했는지 (삭제 버튼 노출 판단용). 비로그인이면 항상 false")
        boolean isMine,
        @Schema(description = "답글 작성자가 피드 원작성자 본인인지 (\"작성자\" 배지 표시용)")
        boolean isAuthor,
        @Schema(description = "작성 시각")
        LocalDateTime createdAt
) {
    public static CommentReplyResponse of(Comment reply, User currentUser) {
        boolean isMine = !reply.isGuestComment()
                && currentUser != null
                && reply.getUser().getId().equals(currentUser.getId());
        String profileImage = reply.isGuestComment() ? reply.getGuestProfileImage() : reply.getUser().getProfileImage();

        return new CommentReplyResponse(
                reply.getId(),
                reply.getParent().getId(),
                reply.isGuestComment() ? "GUEST" : "MEMBER",
                reply.getDisplayNickname(),
                profileImage,
                reply.getContent(),
                isMine,
                reply.isByFeedAuthor(),
                reply.getCreatedAt()
        );
    }
}
