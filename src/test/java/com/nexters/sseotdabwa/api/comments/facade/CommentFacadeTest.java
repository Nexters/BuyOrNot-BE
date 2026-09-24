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
import com.nexters.sseotdabwa.api.comments.dto.CommentGuestDeleteRequest;
import com.nexters.sseotdabwa.api.comments.dto.CommentResponse;
import com.nexters.sseotdabwa.api.votes.dto.VoteRequest;
import com.nexters.sseotdabwa.api.votes.facade.VoteFacade;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.common.response.CursorPageResponse;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.enums.CommentSort;
import com.nexters.sseotdabwa.domain.comments.exception.CommentErrorCode;
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.repository.FeedRepository;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CommentFacadeTest {

    private static final String PROFANE_CONTENT = "시발 진짜 별로다";

    @Autowired
    private CommentFacade commentFacade;

    @Autowired
    private VoteFacade voteFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentRepository commentRepository;

    @AfterEach
    void cleanup() {
        if (!TestTransaction.isActive()) {
            commentRepository.deleteAll();
            feedRepository.deleteAll();
            userRepository.deleteAll();
        }
    }

    // ===== 회원 댓글 작성 =====

    @Test
    @DisplayName("회원 댓글 작성 성공 (투표 여부와 무관)")
    void createComment_success() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);

        // when
        CommentCreateResponse response = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("저도 고민되네요"), ip, null);

        // then
        assertThat(response.id()).isNotNull();
        assertThat(commentRepository.findById(response.id()).orElseThrow().getDisplayNickname())
                .isEqualTo(commenter.getNickname());
    }

    @Test
    @DisplayName("마감된 피드에 회원이 댓글 작성 시 COMMENT_003 에러")
    void createComment_closedFeed_throwsComment003() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        feed.closeVote();
        feedRepository.save(feed);

        // when & then
        assertThatThrownBy(() -> commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null))
                .isInstanceOf(GlobalException.class)
                .hasMessage("마감된 피드에는 댓글을 작성할 수 없습니다.");
    }

    @Test
    @DisplayName("분당 5회 초과 요청 시 COMMENT_010(429) 에러")
    void createComment_rateLimitExceeded_throwsComment010() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        for (int i = 0; i < 5; i++) {
            commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("댓글 " + i), ip, null);
        }

        // when & then
        assertThatThrownBy(() -> commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("6번째 댓글"), ip, null))
                .isInstanceOf(GlobalException.class)
                .hasMessage("잠시 후 다시 댓글을 남길 수 있어요.");
    }

    @Test
    @DisplayName("금칙어 포함 시 COMMENT_011 에러")
    void createComment_profanity_throwsComment011() {
        // given
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);

        // when & then
        assertThatThrownBy(() -> commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest(PROFANE_CONTENT), randomIp(), null))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(CommentErrorCode.COMMENT_PROFANITY_DETECTED);
    }

    @Test
    @DisplayName("10분 내 금칙어 위반 5회 누적 시, 이후 요청은 내용과 무관하게 COMMENT_012(429)로 즉시 차단된다")
    void createComment_repeatedProfanityViolations_throwsComment012() {
        // given: rate limit(IP 기준)에 걸리지 않도록 매 시도마다 IP를 바꾸되, 위반 집계는 회원(userId) 기준이라 그대로 누적된다
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest(PROFANE_CONTENT), randomIp(), null))
                    .isInstanceOf(GlobalException.class)
                    .extracting("errorCode").isEqualTo(CommentErrorCode.COMMENT_PROFANITY_DETECTED);
        }

        // when & then: 6번째는 정상적인 내용이어도 검증 이전에 즉시 차단된다
        assertThatThrownBy(() -> commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("정상적인 댓글"), randomIp(), null))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode").isEqualTo(CommentErrorCode.COMMENT_TEMPORARILY_RESTRICTED);
    }

    // ===== 게스트 댓글 작성 =====

    @Test
    @DisplayName("게스트 댓글 작성 성공 (투표 여부와 무관, 비밀번호 해시 저장)")
    void createGuestComment_success() {
        // given
        String ip = randomIp();
        User owner = createUser();
        Feed feed = createFeed(owner);

        // when
        CommentCreateResponse response = commentFacade.createGuestComment(
                feed.getId(),
                new CommentCreateRequestGuest("저도 고민되네요", "지름신들린수달_1234", "guest-password"),
                ip, null
        );

        // then
        Comment saved = commentRepository.findById(response.id()).orElseThrow();
        assertThat(saved.isGuestComment()).isTrue();
        assertThat(saved.getGuestPasswordHash()).isNotEqualTo("guest-password"); // 해시로 저장됨
    }

    // ===== 회원 댓글 삭제 =====

    @Test
    @DisplayName("본인 댓글 삭제 성공")
    void deleteComment_success() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);

        // when
        commentFacade.deleteComment(commenter, created.id());

        // then
        assertThat(commentRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("본인 댓글이 아니면 삭제 시 COMMENT_005 에러")
    void deleteComment_notOwner_throwsComment005() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        User other = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);

        // when & then
        assertThatThrownBy(() -> commentFacade.deleteComment(other, created.id()))
                .isInstanceOf(GlobalException.class)
                .hasMessage("본인의 댓글만 삭제할 수 있습니다.");
    }

    // ===== 게스트 댓글 삭제 =====

    @Test
    @DisplayName("게스트 댓글 삭제 성공 (비밀번호 일치)")
    void deleteGuestComment_success() {
        // given
        String ip = randomIp();
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createGuestComment(
                feed.getId(), new CommentCreateRequestGuest("내용", "지름신들린수달_1234", "guest-password"), ip, null);

        // when
        commentFacade.deleteGuestComment(created.id(), new CommentGuestDeleteRequest("guest-password"));

        // then
        assertThat(commentRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("게스트 댓글 삭제 시 비밀번호가 틀리면 COMMENT_007 에러")
    void deleteGuestComment_wrongPassword_throwsComment007() {
        // given
        String ip = randomIp();
        User owner = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createGuestComment(
                feed.getId(), new CommentCreateRequestGuest("내용", "지름신들린수달_1234", "guest-password"), ip, null);

        // when & then
        assertThatThrownBy(() -> commentFacade.deleteGuestComment(created.id(), new CommentGuestDeleteRequest("wrong-password")))
                .isInstanceOf(GlobalException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("회원 댓글을 게스트 삭제 API로 삭제 시도하면 COMMENT_006 에러")
    void deleteGuestComment_onMemberComment_throwsComment006() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);

        // when & then
        assertThatThrownBy(() -> commentFacade.deleteGuestComment(created.id(), new CommentGuestDeleteRequest("아무거나")))
                .isInstanceOf(GlobalException.class)
                .hasMessage("비회원이 작성한 댓글이 아닙니다.");
    }

    // ===== 댓글 신고 =====

    @Test
    @DisplayName("댓글 신고 성공 - 1건 신고로 즉시 숨김되어 목록에서 제외된다")
    void reportComment_success_hidesFromList() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        User reporter = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);

        // when
        commentFacade.reportComment(reporter, created.id());

        // then
        assertThat(commentRepository.findById(created.id()).orElseThrow().isReported()).isTrue();
        assertThat(commentFacade.getComments(null, feed.getId(), null, null, null).content()).isEmpty();
    }

    @Test
    @DisplayName("본인 댓글은 자기신고 시 COMMENT_008 에러")
    void reportComment_selfReport_throwsComment008() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);

        // when & then
        assertThatThrownBy(() -> commentFacade.reportComment(commenter, created.id()))
                .isInstanceOf(GlobalException.class)
                .hasMessage("본인의 댓글은 신고할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 신고된 댓글 재신고 시 COMMENT_009 에러")
    void reportComment_alreadyReported_throwsComment009() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        User reporter = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);
        commentFacade.reportComment(reporter, created.id());

        // when & then
        assertThatThrownBy(() -> commentFacade.reportComment(reporter, created.id()))
                .isInstanceOf(GlobalException.class)
                .hasMessage("이미 신고된 댓글입니다.");
    }

    @Test
    @DisplayName("게스트도 인증 없이 댓글 신고가 가능하다")
    void reportComment_byGuestViewer_success() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        CommentCreateResponse created = commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내용"), ip, null);

        // when
        commentFacade.reportComment(null, created.id());

        // then
        assertThat(commentRepository.findById(created.id()).orElseThrow().isReported()).isTrue();
    }

    // ===== 댓글 조회 =====

    @Test
    @DisplayName("댓글 목록은 기본값(등록순, 오래된 순)으로 조회된다")
    void getComments_defaultOrderedByCreatedAsc() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("첫 댓글"), ip, null);
        commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("두번째 댓글"), ip, null);

        // when
        CursorPageResponse<CommentResponse> response = commentFacade.getComments(null, feed.getId(), null, null, null);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).content()).isEqualTo("첫 댓글");
        assertThat(response.content().get(1).content()).isEqualTo("두번째 댓글");
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("sort=LATEST로 조회하면 최신순(최근 작성 순)으로 조회된다")
    void getComments_sortLatest() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        Feed feed = createFeed(owner);
        commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("첫 댓글"), ip, null);
        commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("두번째 댓글"), ip, null);

        // when
        CursorPageResponse<CommentResponse> response = commentFacade.getComments(null, feed.getId(), null, null, CommentSort.LATEST);

        // then
        assertThat(response.content().get(0).content()).isEqualTo("두번째 댓글");
        assertThat(response.content().get(1).content()).isEqualTo("첫 댓글");
    }

    @Test
    @DisplayName("존재하지 않는 피드의 댓글 조회 시 FEED_003 에러")
    void getComments_feedNotFound_throwsFeed003() {
        assertThatThrownBy(() -> commentFacade.getComments(null, 999_999L, null, null, null))
                .isInstanceOf(GlobalException.class)
                .hasMessage("피드를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인이 작성한 댓글은 isMine이 true, 다른 유저/비로그인은 false")
    void getComments_isMineFlag() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User commenter = createUser();
        User otherViewer = createUser();
        Feed feed = createFeed(owner);
        commentFacade.createComment(commenter, feed.getId(), new CommentCreateRequest("내 댓글"), ip, null);

        // when
        CommentResponse asAuthor = commentFacade.getComments(commenter, feed.getId(), null, null, null).content().get(0);
        CommentResponse asOther = commentFacade.getComments(otherViewer, feed.getId(), null, null, null).content().get(0);
        CommentResponse asGuestViewer = commentFacade.getComments(null, feed.getId(), null, null, null).content().get(0);

        // then
        assertThat(asAuthor.isMine()).isTrue();
        assertThat(asOther.isMine()).isFalse();
        assertThat(asGuestViewer.isMine()).isFalse();
    }

    @Test
    @DisplayName("투표한 회원의 댓글은 voteChoice가 채워지고, 미투표 회원/게스트/피드작성자는 비어있다")
    void getComments_voteChoiceAndAuthorTag() {
        // given
        String ip = randomIp();
        User owner = createUser();
        User voter = createUser();
        User notVoter = createUser();
        Feed feed = createFeed(owner);
        voteFacade.vote(voter, feed.getId(), new VoteRequest(VoteChoice.YES));

        commentFacade.createComment(voter, feed.getId(), new CommentCreateRequest("투표한 회원 댓글"), ip, null);
        commentFacade.createComment(notVoter, feed.getId(), new CommentCreateRequest("미투표 회원 댓글"), ip, null);
        commentFacade.createComment(owner, feed.getId(), new CommentCreateRequest("작성자 댓글"), ip, null);
        commentFacade.createGuestComment(feed.getId(), new CommentCreateRequestGuest("게스트 댓글", "지름신들린수달_1234", "pw"), ip, null);

        // when
        var comments = commentFacade.getComments(null, feed.getId(), null, null, null).content();

        // then
        assertThat(comments.get(0).voteChoice()).isEqualTo(VoteChoice.YES);
        assertThat(comments.get(0).isAuthor()).isFalse();

        assertThat(comments.get(1).voteChoice()).isNull();
        assertThat(comments.get(1).isAuthor()).isFalse();

        assertThat(comments.get(2).voteChoice()).isNull();
        assertThat(comments.get(2).isAuthor()).isTrue();

        assertThat(comments.get(3).voteChoice()).isNull();
        assertThat(comments.get(3).isAuthor()).isFalse();
        assertThat(comments.get(3).authorType()).isEqualTo("GUEST");
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

    /**
     * rate limit 카운터가 테스트 간에 공유되지 않도록 테스트마다 고유한 식별자를 사용한다.
     */
    private String randomIp() {
        return "ip-" + UUID.randomUUID();
    }
}
