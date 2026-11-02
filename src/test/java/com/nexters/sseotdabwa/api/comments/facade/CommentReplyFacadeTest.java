package com.nexters.sseotdabwa.api.comments.facade;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentLikeResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentReplyResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentThreadResponse;
import com.nexters.sseotdabwa.api.feeds.facade.FeedFacade;
import com.nexters.sseotdabwa.api.users.facade.UserFacade;
import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.facade.VoteFacade;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.exception.CommentErrorCode;
import com.nexters.sseotdabwa.domain.comments.repository.CommentLikeRepository;
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.comments.service.CommentAggregate;
import com.nexters.sseotdabwa.domain.comments.service.CommentRateLimiter;
import com.nexters.sseotdabwa.domain.comments.service.CommentService;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 답글 / 공감 / 삭제 정책 / 댓글 수 집계
 */
@SpringBootTest
@Transactional
class CommentReplyFacadeTest {

    @Autowired private CommentFacade commentFacade;
    @Autowired private CommentService commentService;
    @Autowired private VoteFacade voteFacade;
    @Autowired private UserFacade userFacade;
    @Autowired private FeedFacade feedFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private FeedRepository feedRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;
    @Autowired private EntityManager entityManager;

    /**
     * 요청 빈도 제한은 CommentFacadeTest에서 검증한다. 여러 테스트 컨텍스트가 같은 H2 DB를 재생성하며 userId가
     * 다시 1부터 발급되면, 캐시된 다른 컨텍스트의 계정 기준 카운터와 겹쳐 무관한 429가 날 수 있어 여기선 끈다.
     */
    @MockBean private CommentRateLimiter commentRateLimiter;

    // ===== 답글 작성 =====

    @Test
    @DisplayName("원댓글에 답글 작성 성공 - parent가 원댓글로 저장된다")
    void createReply_success() {
        User owner = createUser();
        User commenter = createUser();
        User replier = createUser();
        Feed feed = createFeed(owner);
        Long rootId = comment(commenter, feed, "원댓글");

        Long replyId = commentFacade.createReply(replier, feed.getId(), rootId, new CommentCreateRequest("답글")).id();

        Comment reply = commentRepository.findById(replyId).orElseThrow();
        assertThat(reply.isReply()).isTrue();
        assertThat(reply.getParent().getId()).isEqualTo(rootId);
        assertThat(reply.getDisplayNickname()).isEqualTo(replier.getNickname());
    }

    @Test
    @DisplayName("마감된 피드에도 답글 작성 가능")
    void createReply_closedFeed_success() {
        User owner = createUser();
        Feed feed = createFeed(owner);
        Long rootId = comment(createUser(), feed, "원댓글");
        feed.closeVote();
        feedRepository.save(feed);

        Long replyId = commentFacade.createReply(createUser(), feed.getId(), rootId, new CommentCreateRequest("답글")).id();

        assertThat(commentRepository.findById(replyId)).isPresent();
    }

    @Test
    @DisplayName("답글에 답글 작성 시 COMMENT_013 에러")
    void createReply_toReply_throwsComment013() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long replyId = reply(createUser(), feed, rootId, "답글");

        assertThatThrownBy(() -> commentFacade.createReply(createUser(), feed.getId(), replyId, new CommentCreateRequest("답글의 답글")))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_REPLY_TO_REPLY.getMessage());
    }

    @Test
    @DisplayName("신고된 원댓글에 답글 작성 시 COMMENT_014 에러")
    void createReply_toReportedRoot_throwsComment014() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        commentFacade.reportComment(createUser(), rootId);

        assertThatThrownBy(() -> commentFacade.createReply(createUser(), feed.getId(), rootId, new CommentCreateRequest("답글")))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_INVALID_PARENT.getMessage());
    }

    @Test
    @DisplayName("삭제된 원댓글에 답글 작성 시 COMMENT_004 에러 (원댓글이 답글과 함께 삭제됨)")
    void createReply_toDeletedRoot_throwsComment004() {
        Feed feed = createFeed(createUser());
        User commenter = createUser();
        Long rootId = comment(commenter, feed, "원댓글");
        reply(createUser(), feed, rootId, "답글");
        commentFacade.deleteComment(commenter, rootId);
        entityManager.flush();

        assertThatThrownBy(() -> commentFacade.createReply(createUser(), feed.getId(), rootId, new CommentCreateRequest("답글")))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("다른 피드의 댓글 id로 답글 작성 시 COMMENT_004 에러")
    void createReply_parentOfOtherFeed_throwsComment004() {
        Feed feedA = createFeed(createUser());
        Feed feedB = createFeed(createUser());
        Long rootIdOfA = comment(createUser(), feedA, "원댓글");

        assertThatThrownBy(() -> commentFacade.createReply(createUser(), feedB.getId(), rootIdOfA, new CommentCreateRequest("답글")))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    // ===== 게스트 답글 =====

    @Test
    @DisplayName("게스트 답글 작성 성공 - 회원 없이 닉네임/비밀번호 해시/프로필 이미지와 함께 원댓글 밑에 저장된다")
    void createGuestReply_success() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");

        Long replyId = guestReply(feed, rootId, "guest-password");

        Comment reply = commentRepository.findById(replyId).orElseThrow();
        assertThat(reply.isGuestComment()).isTrue();
        assertThat(reply.getParent().getId()).isEqualTo(rootId);
        assertThat(reply.getGuestNickname()).isNotBlank();
        assertThat(reply.getGuestPasswordHash()).isNotEqualTo("guest-password");
        assertThat(reply.getGuestProfileImage()).isNotBlank();

        CommentReplyResponse response = commentFacade.getComments(null, feed.getId(), null, null, null)
                .content().get(0).replies().get(0);
        assertThat(response.authorType()).isEqualTo("GUEST");
        assertThat(response.isMine()).isFalse();
    }

    @Test
    @DisplayName("게스트도 답글에 답글은 달 수 없다 (COMMENT_013)")
    void createGuestReply_toReply_throwsComment013() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long replyId = reply(createUser(), feed, rootId, "답글");

        assertThatThrownBy(() -> guestReply(feed, replyId, "guest-password"))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_REPLY_TO_REPLY.getMessage());
    }

    @Test
    @DisplayName("게스트 답글은 비밀번호가 맞으면 게스트 삭제 API로 삭제되고, 틀리면 COMMENT_007")
    void deleteGuestReply_withPassword() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long replyId = guestReply(feed, rootId, "guest-password");

        assertThatThrownBy(() -> commentFacade.deleteGuestComment(replyId, new CommentGuestDeleteRequest("wrong")))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_GUEST_PASSWORD_MISMATCH.getMessage());

        commentFacade.deleteGuestComment(replyId, new CommentGuestDeleteRequest("guest-password"));
        entityManager.flush();

        assertThat(commentRepository.findById(replyId)).isEmpty();
        assertThat(commentRepository.findById(rootId)).isPresent();
    }

    // ===== 댓글 목록 =====

    @Test
    @DisplayName("댓글 목록은 원댓글만 페이지 대상이고, 각 원댓글에 답글 전체가 등록순으로 붙는다")
    void getComments_rootsWithReplies() {
        Feed feed = createFeed(createUser());
        Long root1 = comment(createUser(), feed, "원댓글1");
        Long root2 = comment(createUser(), feed, "원댓글2");
        Long reply1 = reply(createUser(), feed, root1, "답글1");
        Long reply2 = reply(createUser(), feed, root1, "답글2");

        List<CommentResponse> content = commentFacade.getComments(null, feed.getId(), null, null, null).content();

        assertThat(content).extracting(CommentResponse::id).containsExactly(root1, root2);
        assertThat(content.get(0).replyCount()).isEqualTo(2);
        assertThat(content.get(0).replies()).extracting(CommentReplyResponse::id).containsExactly(reply1, reply2);
        assertThat(content.get(0).replies()).allMatch(r -> r.parentId().equals(root1));
        assertThat(content.get(1).replyCount()).isZero();
        assertThat(content.get(1).replies()).isEmpty();
    }

    @Test
    @DisplayName("원댓글 페이지 크기는 답글 수와 무관하게 원댓글 기준으로 계산된다")
    void getComments_pageSizeCountsRootsOnly() {
        Feed feed = createFeed(createUser());
        Long root1 = comment(createUser(), feed, "원댓글1");
        reply(createUser(), feed, root1, "답글1");
        reply(createUser(), feed, root1, "답글2");
        Long root2 = comment(createUser(), feed, "원댓글2");

        CursorPageResponse<CommentResponse> page = commentFacade.getComments(null, feed.getId(), null, 1, null);

        assertThat(page.content()).extracting(CommentResponse::id).containsExactly(root1);
        assertThat(page.hasNext()).isTrue();
        assertThat(commentFacade.getComments(null, feed.getId(), page.nextCursor(), 1, null).content())
                .extracting(CommentResponse::id).containsExactly(root2);
    }

    @Test
    @DisplayName("신고된 답글은 목록/답글 수에서 제외된다")
    void getComments_excludesReportedReply() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long visible = reply(createUser(), feed, rootId, "보이는 답글");
        Long reported = reply(createUser(), feed, rootId, "신고될 답글");
        commentFacade.reportComment(createUser(), reported);

        CommentResponse root = commentFacade.getComments(null, feed.getId(), null, null, null).content().get(0);

        assertThat(root.replyCount()).isEqualTo(1);
        assertThat(root.replies()).extracting(CommentReplyResponse::id).containsExactly(visible);
    }

    @Test
    @DisplayName("투표한 회원이라도 답글에는 투표 정보가 없고, 같은 사람의 원댓글에는 voteChoice가 노출된다")
    void getComments_voteChoiceOnlyOnRoot() {
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));
        Long rootId = comment(voter, feed, "원댓글");
        reply(voter, feed, rootId, "같은 사람의 답글");

        CommentResponse root = commentFacade.getComments(null, feed.getId(), null, null, null).content().get(0);

        assertThat(root.voteChoice()).isEqualTo(VoteChoice.YES);
        assertThat(root.replies()).hasSize(1); // CommentReplyResponse에는 voteChoice 필드 자체가 없음
    }

    // ===== 답글 페이지 =====

    @Test
    @DisplayName("답글 페이지: totalCount = 원댓글 1 + 답글 수, replies는 빈 배열")
    void getCommentThread_totalCount() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        reply(createUser(), feed, rootId, "답글1");
        reply(createUser(), feed, rootId, "답글2");

        CommentThreadResponse thread = commentFacade.getCommentThread(null, feed.getId(), rootId);

        assertThat(thread.totalCount()).isEqualTo(3);
        assertThat(thread.comment().replyCount()).isEqualTo(2);
        assertThat(thread.comment().replies()).isEmpty();
    }

    @Test
    @DisplayName("답글 페이지: 답글이 없으면 totalCount는 1")
    void getCommentThread_noReplies_totalCountIsOne() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");

        assertThat(commentFacade.getCommentThread(null, feed.getId(), rootId).totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("답글 페이지: 답글 id나 신고된 원댓글로 조회하면 COMMENT_004 에러")
    void getCommentThread_replyOrReported_throwsComment004() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long replyId = reply(createUser(), feed, rootId, "답글");
        Long reportedRootId = comment(createUser(), feed, "신고될 원댓글");
        commentFacade.reportComment(createUser(), reportedRootId);

        assertThatThrownBy(() -> commentFacade.getCommentThread(null, feed.getId(), replyId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
        assertThatThrownBy(() -> commentFacade.getCommentThread(null, feed.getId(), reportedRootId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("답글 목록은 등록순 커서 페이지네이션")
    void getReplies_cursorPagination() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long r1 = reply(createUser(), feed, rootId, "답글1");
        Long r2 = reply(createUser(), feed, rootId, "답글2");
        Long r3 = reply(createUser(), feed, rootId, "답글3");

        CursorPageResponse<CommentReplyResponse> first = commentFacade.getReplies(null, feed.getId(), rootId, null, 2);
        CursorPageResponse<CommentReplyResponse> second = commentFacade.getReplies(null, feed.getId(), rootId, first.nextCursor(), 2);

        assertThat(first.content()).extracting(CommentReplyResponse::id).containsExactly(r1, r2);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.content()).extracting(CommentReplyResponse::id).containsExactly(r3);
        assertThat(second.hasNext()).isFalse();
        assertThat(second.nextCursor()).isNull();
    }

    // ===== 공감 =====

    @Test
    @DisplayName("공감/공감 취소는 멱등이고, 조회자별 isLiked와 likeCount가 반영된다")
    void like_unlike_idempotent() {
        Feed feed = createFeed(createUser());
        User commenter = createUser();
        User liker = createUser();
        Long rootId = comment(commenter, feed, "원댓글");

        commentFacade.likeComment(liker, feed.getId(), rootId);
        CommentLikeResponse again = commentFacade.likeComment(liker, feed.getId(), rootId);
        CommentLikeResponse self = commentFacade.likeComment(commenter, feed.getId(), rootId); // 본인 댓글 공감 허용

        assertThat(again.likeCount()).isEqualTo(1);
        assertThat(self.likeCount()).isEqualTo(2);
        assertThat(commentFacade.getComments(liker, feed.getId(), null, null, null).content().get(0).isLiked()).isTrue();
        assertThat(commentFacade.getComments(createUser(), feed.getId(), null, null, null).content().get(0).isLiked()).isFalse();
        assertThat(commentFacade.getComments(null, feed.getId(), null, null, null).content().get(0).likeCount()).isEqualTo(2);

        CommentLikeResponse unliked = commentFacade.unlikeComment(liker, feed.getId(), rootId);
        CommentLikeResponse unlikedAgain = commentFacade.unlikeComment(liker, feed.getId(), rootId);

        assertThat(unliked.likeCount()).isEqualTo(1);
        assertThat(unlikedAgain.likeCount()).isEqualTo(1);
        assertThat(unlikedAgain.isLiked()).isFalse();
    }

    @Test
    @DisplayName("마감된 피드에서도 공감 가능")
    void like_closedFeed_success() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        feed.closeVote();
        feedRepository.save(feed);

        assertThat(commentFacade.likeComment(createUser(), feed.getId(), rootId).likeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("답글에 공감 시 COMMENT_015 에러")
    void like_onReply_throwsComment015() {
        Feed feed = createFeed(createUser());
        Long rootId = comment(createUser(), feed, "원댓글");
        Long replyId = reply(createUser(), feed, rootId, "답글");

        assertThatThrownBy(() -> commentFacade.likeComment(createUser(), feed.getId(), replyId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_LIKE_ON_REPLY.getMessage());
    }

    @Test
    @DisplayName("삭제된 원댓글에 공감 시 COMMENT_004 에러")
    void like_onDeletedRoot_throwsComment004() {
        Feed feed = createFeed(createUser());
        User commenter = createUser();
        Long rootId = comment(commenter, feed, "원댓글");
        reply(createUser(), feed, rootId, "답글");
        commentFacade.deleteComment(commenter, rootId);
        entityManager.flush();

        assertThatThrownBy(() -> commentFacade.likeComment(createUser(), feed.getId(), rootId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    // ===== 삭제 정책 =====

    @Test
    @DisplayName("답글이 없는 원댓글은 공감과 함께 hard delete된다")
    void delete_rootWithoutReplies_hardDelete() {
        Feed feed = createFeed(createUser());
        User commenter = createUser();
        Long rootId = comment(commenter, feed, "원댓글");
        commentFacade.likeComment(createUser(), feed.getId(), rootId);

        commentFacade.deleteComment(commenter, rootId);
        entityManager.flush();

        assertThat(commentRepository.findById(rootId)).isEmpty();
        assertThat(commentLikeRepository.countByCommentId(rootId)).isZero();
    }

    @Test
    @DisplayName("답글이 있는 원댓글을 삭제하면 답글(신고된 답글 포함)과 공감까지 모두 삭제된다")
    void delete_rootWithReplies_deletesWholeThread() {
        Feed feed = createFeed(createUser());
        User commenter = createUser();
        Long rootId = comment(commenter, feed, "원댓글");
        Long replyId = reply(createUser(), feed, rootId, "답글");
        Long reportedReplyId = reply(createUser(), feed, rootId, "신고될 답글");
        commentFacade.reportComment(createUser(), reportedReplyId);
        commentFacade.likeComment(createUser(), feed.getId(), rootId);

        commentFacade.deleteComment(commenter, rootId);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(rootId)).isEmpty();
        assertThat(commentRepository.findById(replyId)).isEmpty();
        assertThat(commentRepository.findById(reportedReplyId)).isEmpty();
        assertThat(commentLikeRepository.countByCommentId(rootId)).isZero();
        assertThat(commentFacade.getComments(null, feed.getId(), null, null, null).content()).isEmpty();
        assertThatThrownBy(() -> commentFacade.getCommentThread(null, feed.getId(), rootId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게스트 원댓글도 비밀번호로 삭제하면 답글까지 모두 삭제된다")
    void deleteGuest_rootWithReplies_deletesWholeThread() {
        Feed feed = createFeed(createUser());
        Long rootId = commentFacade.createGuestComment(feed.getId(),
                new CommentCreateRequestGuest("게스트 원댓글", "지름신들린수달_1234", "pw1234"),
                "ip-" + UUID.randomUUID(), null).id();
        Long replyId = reply(createUser(), feed, rootId, "답글");

        commentFacade.deleteGuestComment(rootId, new CommentGuestDeleteRequest("pw1234"));
        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(rootId)).isEmpty();
        assertThat(commentRepository.findById(replyId)).isEmpty();
    }

    @Test
    @DisplayName("답글을 삭제하면 해당 답글만 삭제되고 원댓글과 다른 답글은 유지된다")
    void delete_reply_deletesOnlyReply() {
        Feed feed = createFeed(createUser());
        User replier = createUser();
        Long rootId = comment(createUser(), feed, "원댓글");
        Long replyId = reply(replier, feed, rootId, "지울 답글");
        Long otherReplyId = reply(createUser(), feed, rootId, "남는 답글");

        commentFacade.deleteComment(replier, replyId);
        entityManager.flush();
        entityManager.clear();

        CommentResponse root = commentFacade.getComments(null, feed.getId(), null, null, null).content().get(0);
        assertThat(root.id()).isEqualTo(rootId);
        assertThat(root.replyCount()).isEqualTo(1);
        assertThat(root.replies()).extracting(CommentReplyResponse::id).containsExactly(otherReplyId);
        assertThat(commentFacade.getCommentThread(null, feed.getId(), rootId).totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("이미 삭제된 원댓글을 다시 삭제하면 COMMENT_004 에러")
    void delete_alreadyDeleted_throwsComment004() {
        Feed feed = createFeed(createUser());
        User commenter = createUser();
        Long rootId = comment(commenter, feed, "원댓글");
        reply(createUser(), feed, rootId, "답글");
        commentFacade.deleteComment(commenter, rootId);
        entityManager.flush();

        assertThatThrownBy(() -> commentFacade.deleteComment(commenter, rootId))
                .isInstanceOf(GlobalException.class)
                .hasMessage(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    // ===== 피드 댓글 수 / 최신 댓글 집계 =====

    @Test
    @DisplayName("commentCount는 원댓글+답글, 신고된 댓글/답글과 신고된 원댓글 밑 답글, 삭제된 원댓글의 답글은 제외. latestComment는 최신 원댓글")
    void aggregate_countsRootsAndReplies() {
        Feed feed = createFeed(createUser());
        User deletedRootAuthor = createUser();

        Long root1 = comment(createUser(), feed, "원댓글1");
        reply(createUser(), feed, root1, "답글1-1");                       // +1
        Long reportedReply = reply(createUser(), feed, root1, "답글1-2");  // 신고 → 제외
        Long reportedRoot = comment(createUser(), feed, "신고될 원댓글");   // 신고 → 제외
        reply(createUser(), feed, reportedRoot, "신고된 원댓글의 답글");     // 제외
        Long deletedRoot = comment(deletedRootAuthor, feed, "삭제될 원댓글"); // 삭제 → 제외
        reply(createUser(), feed, deletedRoot, "삭제된 원댓글의 답글");      // 함께 삭제 → 제외
        Long latestRoot = comment(createUser(), feed, "최신 원댓글");        // +1
        reply(createUser(), feed, latestRoot, "최신 원댓글의 답글");         // +1 (답글은 latestComment 후보 아님)

        commentFacade.reportComment(createUser(), reportedReply);
        commentFacade.reportComment(createUser(), reportedRoot);
        commentFacade.deleteComment(deletedRootAuthor, deletedRoot);
        entityManager.flush();

        Map<Long, CommentAggregate> aggregates = commentService.aggregateByFeedIds(List.of(feed.getId()));

        // root1 + 답글1-1 + 최신 원댓글 + 최신 원댓글의 답글 = 4
        assertThat(aggregates.get(feed.getId()).commentCount()).isEqualTo(4);
        assertThat(aggregates.get(feed.getId()).latestComment().getId()).isEqualTo(latestRoot);
    }

    // ===== 피드 삭제 / 회원 탈퇴 (FK 순서) =====

    @Test
    @DisplayName("답글/공감이 달린 피드를 삭제해도 FK 오류 없이 모두 삭제된다")
    void deleteFeed_withRepliesAndLikes() {
        User owner = createUser();
        Feed feed = createFeed(owner);
        Long rootId = comment(createUser(), feed, "원댓글");
        reply(createUser(), feed, rootId, "답글");
        commentFacade.likeComment(createUser(), feed.getId(), rootId);
        entityManager.flush();

        feedFacade.deleteFeed(owner, feed.getId());
        entityManager.flush();
        entityManager.clear(); // JPQL 일괄 삭제는 1차 캐시를 비우지 않으므로 DB 기준으로 다시 조회

        assertThat(commentRepository.findById(rootId)).isEmpty();
        assertThat(feedRepository.findById(feed.getId())).isEmpty();
    }

    @Test
    @DisplayName("남의 답글이 달린 내 원댓글이 있어도 회원 탈퇴가 FK 오류 없이 처리된다")
    void withdraw_userWithRootHavingOthersReplies() {
        Feed othersFeed = createFeed(createUser());
        User leaving = createUser();
        User other = createUser();
        Long myRoot = comment(leaving, othersFeed, "내 원댓글");
        Long othersReplyOnMyRoot = reply(other, othersFeed, myRoot, "남의 답글");
        Long othersRoot = comment(other, othersFeed, "남의 원댓글");
        Long myReplyOnOthersRoot = reply(leaving, othersFeed, othersRoot, "내 답글");
        commentFacade.likeComment(other, othersFeed.getId(), myRoot);
        commentFacade.likeComment(leaving, othersFeed.getId(), othersRoot);
        entityManager.flush();

        userFacade.withdraw(leaving);
        entityManager.flush();
        entityManager.clear(); // JPQL 일괄 삭제는 1차 캐시를 비우지 않으므로 DB 기준으로 다시 조회

        assertThat(commentRepository.findById(myRoot)).isEmpty();
        assertThat(commentRepository.findById(othersReplyOnMyRoot)).isEmpty();
        assertThat(commentRepository.findById(myReplyOnOthersRoot)).isEmpty();
        assertThat(commentRepository.findById(othersRoot)).isPresent();
        assertThat(commentLikeRepository.countByCommentId(othersRoot)).isZero();
    }

    // ===== Helper Methods =====

    private Long comment(User user, Feed feed, String content) {
        return commentFacade.createComment(user, feed.getId(), new CommentCreateRequest(content)).id();
    }

    private Long reply(User user, Feed feed, Long rootId, String content) {
        return commentFacade.createReply(user, feed.getId(), rootId, new CommentCreateRequest(content)).id();
    }

    private Long guestReply(Feed feed, Long rootId, String password) {
        return commentFacade.createGuestReply(
                feed.getId(), rootId,
                new CommentCreateRequestGuest("게스트 답글", "지름신들린수달_1234", password),
                "ip-" + UUID.randomUUID(), null
        ).id();
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private Feed createFeed(User user) {
        return feedRepository.save(Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build());
    }
}
