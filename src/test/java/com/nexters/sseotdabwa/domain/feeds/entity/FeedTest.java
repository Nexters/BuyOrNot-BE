package com.nexters.sseotdabwa.domain.feeds.entity;

import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FeedTest {

    @Test
    @DisplayName("48시간 이내 피드는 만료되지 않음")
    void isExpired_withinDeadline_returnsFalse() throws Exception {
        // given
        Feed feed = createFeed();
        setCreatedAt(feed, LocalDateTime.now().minusHours(47));

        // when & then
        assertThat(feed.isExpired()).isFalse();
    }

    @Test
    @DisplayName("48시간 초과 피드는 만료됨")
    void isExpired_afterDeadline_returnsTrue() throws Exception {
        // given
        Feed feed = createFeed();
        setCreatedAt(feed, LocalDateTime.now().minusHours(49));

        // when & then
        assertThat(feed.isExpired()).isTrue();
    }

    @Test
    @DisplayName("createdAt이 null이면 만료되지 않음")
    void isExpired_nullCreatedAt_returnsFalse() {
        // given
        Feed feed = createFeed();

        // when & then
        assertThat(feed.isExpired()).isFalse();
    }

    private Feed createFeed() {
        User user = User.builder()
                .socialId("test-social-id")
                .nickname("테스트")
                .socialAccount(SocialAccount.KAKAO)
                .build();

        return Feed.builder()
                .user(user)
                .content("테스트 피드")
                .price(10000L)
                .category(FeedCategory.FASHION)
                .imageWidth(300)
                .imageHeight(400)
                .build();
    }

    private void setCreatedAt(Feed feed, LocalDateTime createdAt) throws Exception {
        Field field = feed.getClass().getSuperclass().getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(feed, createdAt);
    }
}
