package com.nexters.sseotdabwa.api.comments.dto;

import java.time.LocalDateTime;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글(의견) 응답")
public record CommentResponse(
        @Schema(description = "댓글 id")
        Long id,
        @Schema(description = "작성자 유형. MEMBER(회원) 또는 GUEST(게스트)")
        String authorType,
        @Schema(description = "작성 시점 닉네임 스냅샷. 회원 닉네임이 나중에 바뀌어도 과거 댓글 표시는 바뀌지 않는다.")
        String nickname,
        @Schema(description = "작성자 프로필 이미지 URL. 게스트는 작성 시점에 랜덤 배정되어 고정된다.")
        String profileImage,
        @Schema(description = "댓글 내용")
        String content,
        @Schema(description = "현재 조회 중인 회원 본인이 작성한 댓글인지. 게스트는 세션이 없어 조회 시 항상 false.")
        boolean isMine,
        @Schema(description = "이 댓글의 작성자가 피드 원작성자 본인인지. true면 FE에서 투표 태그 대신 \"작성자\" 배지를 표시한다.")
        boolean isAuthor,
        @Schema(
                description = "댓글 작성자(회원)가 이 피드에 투표했다면 그 선택(YES/NO). 게스트 댓글이거나 투표하지 않은 회원의 댓글, "
                        + "또는 피드 작성자 본인의 댓글(자기 피드에는 투표 불가)이면 항상 null — FE는 null이면 투표 태그/버블을 노출하지 않는다."
        )
        VoteChoice voteChoice,
        @Schema(description = "작성 시각")
        LocalDateTime createdAt
) {
    /**
     * @param voteChoice 댓글 작성자(회원)가 이 피드에 투표했다면 그 선택. 게스트 또는 미투표 회원은 null.
     */
    public static CommentResponse of(Comment comment, User currentUser, String profileImage, VoteChoice voteChoice) {
        boolean isMine = !comment.isGuestComment()
                && currentUser != null
                && comment.getUser().getId().equals(currentUser.getId());
        boolean isAuthor = comment.isByFeedAuthor();

        return new CommentResponse(
                comment.getId(),
                comment.isGuestComment() ? "GUEST" : "MEMBER",
                comment.getDisplayNickname(),
                profileImage,
                comment.getContent(),
                isMine,
                isAuthor,
                isAuthor ? null : voteChoice,
                comment.getCreatedAt()
        );
    }
}
