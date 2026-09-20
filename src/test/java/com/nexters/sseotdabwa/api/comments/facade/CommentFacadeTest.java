package com.nexters.sseotdabwa.api.comments.facade;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateRequestGuest;
import com.nexters.sseotdabwa.api.comments.dto.CommentCreateResponse;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.dto.VoteResponse;
import com.nexters.sseotdabwa.api.votes.facade.VoteFacade;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.repository.VoteLogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CommentFacadeTest {

    @Autowired
    private CommentFacade commentFacade;

    @Autowired
    private VoteFacade voteFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private VoteLogRepository voteLogRepository;

    @Autowired
    private CommentRepository commentRepository;

    @AfterEach
    void cleanup() {
        if (!TestTransaction.isActive()) {
            commentRepository.deleteAll();
            voteLogRepository.deleteAll();
            feedRepository.deleteAll();
            userRepository.deleteAll();
        }
    }

    // ===== 회원 댓글 =====

    @Test
    @DisplayName("투표한 회원의 댓글 작성 성공")
    void createComment_success() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));

        // when
        CommentCreateResponse response = commentFacade.createComment(voter, feed.getId(), new CommentCreateRequest("저도 고민되네요"));

        // then
        assertThat(response.id()).isNotNull();
        assertThat(commentRepository.findById(response.id()).orElseThrow().getDisplayNickname())
                .isEqualTo(voter.getNickname());
    }

    @Test
    @DisplayName("투표하지 않은 회원이 댓글 작성 시 COMMENT_004 에러")
    void createComment_notVoted_throwsComment004() {
        // given
        User owner = createUser();
        User notVoter = createUser();
        Feed feed = createFeed(owner);

        // when & then
        assertThatThrownBy(() -> commentFacade.createComment(notVoter, feed.getId(), new CommentCreateRequest("내용")))
                .isInstanceOf(GlobalException.class)
                .hasMessage("투표한 피드에만 댓글을 작성할 수 있습니다.");
    }

    @Test
    @DisplayName("마감된 피드에 회원이 댓글 작성 시 COMMENT_003 에러")
    void createComment_closedFeed_throwsComment003() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));
        feed.closeVote();
        feedRepository.save(feed);

        // when & then
        assertThatThrownBy(() -> commentFacade.createComment(voter, feed.getId(), new CommentCreateRequest("내용")))
                .isInstanceOf(GlobalException.class)
                .hasMessage("마감된 피드에는 댓글을 작성할 수 없습니다.");
    }

    // ===== 게스트 댓글 =====

    @Test
    @DisplayName("투표한 게스트의 댓글 작성 성공 (voteToken 검증)")
    void createGuestComment_success() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);
        VoteResponse voteResponse = voteFacade.guestVote(feed.getId(), new VoteRequest(VoteChoice.YES));

        // when
        CommentCreateResponse response = commentFacade.createGuestComment(
                feed.getId(),
                new CommentCreateRequestGuest("저도 고민되네요", "지름신들린수달_1234", voteResponse.voteToken())
        );

        // then
        assertThat(response.id()).isNotNull();
        assertThat(commentRepository.findById(response.id()).orElseThrow().isGuestComment()).isTrue();
    }

    @Test
    @DisplayName("다른 피드에서 발급된 voteToken으로 게스트 댓글 작성 시 COMMENT_005 에러")
    void createGuestComment_voteTokenForOtherFeed_throwsComment005() {
        // given
        User owner = createUser();
        Feed feed1 = createFeed(owner);
        Feed feed2 = createFeed(owner);
        VoteResponse voteResponseForFeed1 = voteFacade.guestVote(feed1.getId(), new VoteRequest(VoteChoice.YES));

        // when & then
        assertThatThrownBy(() -> commentFacade.createGuestComment(
                feed2.getId(),
                new CommentCreateRequestGuest("내용", "지름신들린수달_1234", voteResponseForFeed1.voteToken())
        ))
                .isInstanceOf(GlobalException.class)
                .hasMessage("유효하지 않거나 만료된 투표 인증입니다.");
    }

    @Test
    @DisplayName("voteToken 없이 게스트 댓글 작성 시 COMMENT_005 에러")
    void createGuestComment_blankVoteToken_throwsComment005() {
        // given
        User owner = createUser();
        Feed feed = createFeed(owner);

        // when & then
        assertThatThrownBy(() -> commentFacade.createGuestComment(
                feed.getId(),
                new CommentCreateRequestGuest("내용", "지름신들린수달_1234", "")
        ))
                .isInstanceOf(GlobalException.class)
                .hasMessage("유효하지 않거나 만료된 투표 인증입니다.");
    }

    // ===== 댓글 조회 =====

    @Test
    @DisplayName("댓글 목록은 등록순(오래된 순)으로 조회된다")
    void getComments_orderedByCreatedAsc() {
        // given
        User owner = createUser();
        User voter = createUser();
        Feed feed = createFeed(owner);
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));
        commentFacade.createComment(voter, feed.getId(), new CommentCreateRequest("첫 댓글"));
        commentFacade.createComment(voter, feed.getId(), new CommentCreateRequest("두번째 댓글"));

        // when
        CursorPageResponse<CommentResponse> response = commentFacade.getComments(null, feed.getId(), null, null);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).content()).isEqualTo("첫 댓글");
        assertThat(response.content().get(1).content()).isEqualTo("두번째 댓글");
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 피드의 댓글 조회 시 FEED_003 에러")
    void getComments_feedNotFound_throwsFeed003() {
        assertThatThrownBy(() -> commentFacade.getComments(null, 999_999L, null, null))
                .isInstanceOf(GlobalException.class)
                .hasMessage("피드를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인이 작성한 댓글은 isMine이 true, 다른 유저/비로그인은 false")
    void getComments_isMineFlag() {
        // given
        User owner = createUser();
        User voter = createUser();
        User otherViewer = createUser();
        Feed feed = createFeed(owner);
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));
        commentFacade.createComment(voter, feed.getId(), new CommentCreateRequest("내 댓글"));

        // when
        CommentResponse asAuthor = commentFacade.getComments(voter, feed.getId(), null, null).content().get(0);
        CommentResponse asOther = commentFacade.getComments(otherViewer, feed.getId(), null, null).content().get(0);
        CommentResponse asGuestViewer = commentFacade.getComments(null, feed.getId(), null, null).content().get(0);

        // then
        assertThat(asAuthor.isMine()).isTrue();
        assertThat(asOther.isMine()).isFalse();
        assertThat(asGuestViewer.isMine()).isFalse();
    }

    // ===== Helper Methods =====

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
