package com.nexters.sseotdabwa.api.comments.dto;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 피드 목록/상세 응답에 얹는 "가장 최근 댓글 1개" 미리보기.
 * isAuthor/voteChoice는 CommentResponse와 동일 규칙 — FE가 태그/버블을 프리뷰에 노출할지는 별도 판단.
 */
@Schema(description = "피드의 가장 최근 댓글 1개 미리보기 (댓글이 없으면 이 필드 자체가 null)")
public record CommentPreviewResponse(
        @Schema(description = "작성자 유형. MEMBER(회원) 또는 GUEST(게스트)")
        String authorType,
        @Schema(description = "작성 시점 닉네임 스냅샷")
        String nickname,
        @Schema(description = "작성자 프로필 이미지 URL")
        String profileImage,
        @Schema(description = "댓글 내용")
        String content,
        @Schema(description = "이 댓글의 작성자가 피드 원작성자 본인인지")
        boolean isAuthor,
        @Schema(description = "댓글 작성자(회원)의 이 피드 투표 선택(YES/NO). 게스트이거나 미투표/작성자 본인이면 null")
        VoteChoice voteChoice
) {
    public static CommentPreviewResponse of(Comment comment, String profileImage, VoteChoice voteChoice) {
        boolean isAuthor = comment.isByFeedAuthor();
        return new CommentPreviewResponse(
                comment.isGuestComment() ? "GUEST" : "MEMBER",
                comment.getDisplayNickname(),
                profileImage,
                comment.getContent(),
                isAuthor,
                isAuthor ? null : voteChoice
        );
    }
}
