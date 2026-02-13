package com.nexters.sseotdabwa.domain.feeds.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "feed_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_feed_review_feed", columnNames = {"feed_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false, unique = true)
    private Feed feed;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Builder
    public FeedReview(Feed feed, String content) {
        this.feed = feed;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
