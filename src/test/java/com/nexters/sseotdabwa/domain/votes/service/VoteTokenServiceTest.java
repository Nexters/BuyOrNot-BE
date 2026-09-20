package com.nexters.sseotdabwa.domain.votes.service;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;

import static org.assertj.core.api.Assertions.assertThat;

class VoteTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-key-for-vote-token-must-be-long-enough";

    private final VoteTokenService voteTokenService = new VoteTokenService(SECRET);

    @Test
    @DisplayName("발급한 voteToken은 같은 feedId에 대해 유효하다")
    void createGuestVoteToken_validForSameFeed() {
        // given
        Feed feed = createFeed(1L, LocalDateTime.now());

        // when
        String token = voteTokenService.createGuestVoteToken(feed);

        // then
        assertThat(voteTokenService.isValid(token, 1L)).isTrue();
    }

    @Test
    @DisplayName("다른 feedId로는 voteToken이 유효하지 않다")
    void isValid_differentFeedId_returnsFalse() {
        // given
        Feed feed = createFeed(1L, LocalDateTime.now());
        String token = voteTokenService.createGuestVoteToken(feed);

        // when & then
        assertThat(voteTokenService.isValid(token, 2L)).isFalse();
    }

    @Test
    @DisplayName("마감(48시간 초과)된 피드로 발급된 voteToken은 만료되어 유효하지 않다")
    void isValid_expiredToken_returnsFalse() {
        // given - 이미 마감 시각이 지난 피드로 발급된 토큰
        Feed feed = createFeed(1L, LocalDateTime.now().minusHours(100));
        String token = voteTokenService.createGuestVoteToken(feed);

        // when & then
        assertThat(voteTokenService.isValid(token, 1L)).isFalse();
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된(위조된) 토큰은 유효하지 않다")
    void isValid_tamperedToken_returnsFalse() {
        // given
        VoteTokenService otherService = new VoteTokenService("another-completely-different-jwt-secret-key-value");
        Feed feed = createFeed(1L, LocalDateTime.now());
        String token = otherService.createGuestVoteToken(feed);

        // when & then
        assertThat(voteTokenService.isValid(token, 1L)).isFalse();
    }

    @Test
    @DisplayName("빈 문자열/null 토큰은 유효하지 않다")
    void isValid_blankOrNullToken_returnsFalse() {
        assertThat(voteTokenService.isValid("", 1L)).isFalse();
        assertThat(voteTokenService.isValid(null, 1L)).isFalse();
        assertThat(voteTokenService.isValid("not-a-jwt", 1L)).isFalse();
    }

    private Feed createFeed(Long id, LocalDateTime createdAt) {
        Feed feed = Feed.builder()
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .build();
        ReflectionTestUtils.setField(feed, "id", id);
        ReflectionTestUtils.setField(feed, "createdAt", createdAt);
        return feed;
    }
}
