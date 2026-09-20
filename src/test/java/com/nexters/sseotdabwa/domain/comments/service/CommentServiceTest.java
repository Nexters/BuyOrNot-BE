package com.nexters.sseotdabwa.domain.comments.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.comments.entity.Comment;
import com.nexters.sseotdabwa.domain.comments.repository.CommentRepository;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.service.RandomNicknameGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RandomNicknameGenerator randomNicknameGenerator;

    @InjectMocks
    private CommentService commentService;

    // ===== 회원 댓글 =====

    @Test
    @DisplayName("회원 댓글 작성 성공 - displayNickname은 user.nickname을 그대로 사용한다")
    void createMemberComment_success() {
        // given
        Feed feed = createFeed(1L);
        User user = createUser("참새방앗간12345");
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Comment comment = commentService.createMemberComment(feed, user, "저도 고민되네요");

        // then
        assertThat(comment.getDisplayNickname()).isEqualTo("참새방앗간12345");
        assertThat(comment.isGuestComment()).isFalse();
        assertThat(comment.getContent()).isEqualTo("저도 고민되네요");
    }

    @Test
    @DisplayName("회원 댓글 내용이 공백이면 COMMENT_001 에러")
    void createMemberComment_blankContent_throwsComment001() {
        // given
        Feed feed = createFeed(1L);
        User user = createUser("참새방앗간12345");

        // when & then
        assertThatThrownBy(() -> commentService.createMemberComment(feed, user, "   "))
                .isInstanceOf(GlobalException.class)
                .hasMessage("댓글 내용을 입력해주세요.");
    }

    @Test
    @DisplayName("회원 댓글 내용이 100자를 초과하면 COMMENT_002 에러")
    void createMemberComment_tooLongContent_throwsComment002() {
        // given
        Feed feed = createFeed(1L);
        User user = createUser("참새방앗간12345");
        String tooLong = "가".repeat(101);

        // when & then
        assertThatThrownBy(() -> commentService.createMemberComment(feed, user, tooLong))
                .isInstanceOf(GlobalException.class)
                .hasMessage("댓글은 100자 이하로 입력해주세요.");
    }

    // ===== 게스트 댓글 =====

    @Test
    @DisplayName("게스트 댓글 - 조작된 닉네임은 서버가 새로 발급한다")
    void createGuestComment_invalidNickname_regenerates() {
        // given
        Feed feed = createFeed(1L);
        when(randomNicknameGenerator.isValid("해킹시도닉네임")).thenReturn(false);
        when(randomNicknameGenerator.generate()).thenReturn("지름신들린수달_1234");
        when(commentRepository.existsByFeedIdAndDisplayNickname(1L, "지름신들린수달_1234")).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Comment comment = commentService.createGuestComment(feed, "해킹시도닉네임", "https://img.example.com/1.png", "내용");

        // then
        assertThat(comment.getDisplayNickname()).isEqualTo("지름신들린수달_1234");
        assertThat(comment.getGuestNickname()).isEqualTo("지름신들린수달_1234");
        assertThat(comment.isGuestComment()).isTrue();
        verify(randomNicknameGenerator, times(1)).generate();
    }

    @Test
    @DisplayName("게스트 댓글 - 같은 피드 안에서 닉네임이 겹치면 재발급한다")
    void createGuestComment_nicknameCollisionInFeed_retriesUntilUnique() {
        // given
        Feed feed = createFeed(1L);
        when(randomNicknameGenerator.isValid("지름신들린수달_1234")).thenReturn(true);
        when(commentRepository.existsByFeedIdAndDisplayNickname(1L, "지름신들린수달_1234")).thenReturn(true);
        when(randomNicknameGenerator.generate()).thenReturn("무지출챌린저_5678");
        when(commentRepository.existsByFeedIdAndDisplayNickname(1L, "무지출챌린저_5678")).thenReturn(false);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Comment comment = commentService.createGuestComment(feed, "지름신들린수달_1234", null, "내용");

        // then
        assertThat(comment.getDisplayNickname()).isEqualTo("무지출챌린저_5678");
    }

    @Test
    @DisplayName("게스트 댓글 내용이 공백이면 COMMENT_001 에러")
    void createGuestComment_blankContent_throwsComment001() {
        // given
        Feed feed = createFeed(1L);

        // when & then
        assertThatThrownBy(() -> commentService.createGuestComment(feed, "아무닉네임", null, ""))
                .isInstanceOf(GlobalException.class)
                .hasMessage("댓글 내용을 입력해주세요.");
    }

    // ===== Helper Methods =====

    private Feed createFeed(Long id) {
        Feed feed = Feed.builder()
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build();
        ReflectionTestUtils.setField(feed, "id", id);
        return feed;
    }

    private User createUser(String nickname) {
        return User.builder()
                .socialId("social-id")
                .nickname(nickname)
                .socialAccount(SocialAccount.KAKAO)
                .build();
    }
}
