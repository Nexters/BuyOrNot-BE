package com.nexters.sseotdabwa.api.comments.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글(의견, 원댓글) 응답")
public record CommentResponse(
        @Schema(description = "댓글 id")
        Long id,
        @Schema(description = "작성자 유형. MEMBER(회원) 또는 GUEST(게스트)")
        String authorType,
        @Schema(description = "작성 시점 닉네임 스냅샷. 회원 닉네임이 나중에 바뀌어도 과거 댓글 표시는 바뀌지 않는다. 삭제된 댓글이면 null")
        String nickname,
        @Schema(description = "작성자 프로필 이미지 URL. 게스트는 작성 시점에 랜덤 배정되어 고정된다. 삭제된 댓글이면 null")
        String profileImage,
        @Schema(description = "댓글 내용. 삭제된 댓글이면 null")
        String content,
        @Schema(
                description = "이 댓글을 지금 조회 중인 회원 본인이 작성했는지 (삭제 버튼 노출 판단용). 게스트로 조회하거나 "
                        + "비로그인 상태면 항상 false. isAuthor(피드 원작성자 여부, 조회자와 무관하게 고정)와는 다른 개념이니 혼동 주의."
        )
        boolean isMine,
        @Schema(
                description = "이 댓글의 작성자가 피드 원작성자 본인인지 (\"작성자\" 배지 표시용). 누가 조회하든 값이 고정된다는 점에서 "
                        + "isMine(조회자 본인 여부, 조회자마다 달라짐)과 다르다."
        )
        boolean isAuthor,
        @Schema(
                description = "댓글 작성자(회원)가 이 피드에 투표했다면 그 선택(YES/NO). 게스트 댓글이거나 투표하지 않은 회원의 댓글, "
                        + "또는 피드 작성자 본인의 댓글(자기 피드에는 투표 불가)이면 항상 null — FE는 null이면 투표 태그/버블을 노출하지 않는다."
        )
        VoteChoice voteChoice,
        @Schema(description = "작성 시각")
        LocalDateTime createdAt,
        @Schema(
                description = "답글이 남아있는 상태에서 삭제된 원댓글인지. true면 \"삭제된 댓글입니다\"로 표시하고 "
                        + "nickname/profileImage/content/voteChoice는 null, isMine/isAuthor/isLiked는 false, likeCount는 0으로 내려간다."
        )
        boolean isDeleted,
        @Schema(description = "공감 수 (0이면 \"공감\" 텍스트, 99 초과 표기는 FE)")
        long likeCount,
        @Schema(description = "지금 조회 중인 회원이 공감했는지. 비로그인이면 항상 false")
        boolean isLiked,
        @Schema(description = "답글 수 (신고된 답글 제외, 0이면 \"답글쓰기\" 텍스트)")
        long replyCount,
        @Schema(description = "답글 전체 목록 (등록순). 답글 페이지 조회(GET .../comments/{commentId})에서는 빈 배열이며, 답글은 .../replies로 조회한다")
        List<CommentReplyResponse> replies
) {
    /**
     * @param voteChoice 댓글 작성자(회원)가 이 피드에 투표했다면 그 선택. 게스트 또는 미투표 회원은 null.
     */
    public static CommentResponse of(Comment comment, User currentUser, String profileImage, VoteChoice voteChoice,
                                     long likeCount, boolean isLiked, long replyCount, List<CommentReplyResponse> replies) {
        String authorType = comment.isGuestComment() ? "GUEST" : "MEMBER";

        if (comment.isDeleted()) {
            return new CommentResponse(
                    comment.getId(), authorType, null, null, null,
                    false, false, null, comment.getCreatedAt(),
                    true, 0L, false, replyCount, replies
            );
        }

        boolean isMine = !comment.isGuestComment()
                && currentUser != null
                && comment.getUser().getId().equals(currentUser.getId());
        boolean isAuthor = comment.isByFeedAuthor();

        return new CommentResponse(
                comment.getId(),
                authorType,
                comment.getDisplayNickname(),
                profileImage,
                comment.getContent(),
                isMine,
                isAuthor,
                isAuthor ? null : voteChoice,
                comment.getCreatedAt(),
                false,
                likeCount,
                isLiked,
                replyCount,
                replies
        );
    }
}
