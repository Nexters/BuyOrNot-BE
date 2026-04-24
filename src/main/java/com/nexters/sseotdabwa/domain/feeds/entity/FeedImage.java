package com.nexters.sseotdabwa.domain.feeds.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Column(name = "s3_object_key", nullable = false, length = 255)
    private String s3ObjectKey;

    @Column(nullable = false)
    private Integer imageWidth;

    @Column(nullable = false)
    private Integer imageHeight;

    @Builder
    public FeedImage(Feed feed, String s3ObjectKey, Integer imageWidth, Integer imageHeight) {
        this.feed = feed;
        this.s3ObjectKey = s3ObjectKey;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }
}
