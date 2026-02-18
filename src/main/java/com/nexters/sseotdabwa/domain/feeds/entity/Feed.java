package com.nexters.sseotdabwa.domain.feeds.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedCategory;
import com.nexters.sseotdabwa.domain.feeds.enums.FeedStatus;
import com.nexters.sseotdabwa.domain.feeds.enums.ReportStatus;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseEntity {

    private static final long VOTE_DEADLINE_HOURS = 48;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus reportStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedCategory category;

    @Column(nullable = false)
    private Long yesCount;

    @Column(nullable = false)
    private Long noCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedStatus feedStatus;

    @Column(nullable = false)
    private Integer imageWidth;

    @Column(nullable = false)
    private Integer imageHeight;

    @Builder
    public Feed(User user, String content, Long price, FeedCategory category, Integer imageWidth, Integer imageHeight) {
        this.user = user;
        this.content = content;
        this.price = price;
        this.category = category;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.reportStatus = ReportStatus.NONE;
        this.feedStatus = FeedStatus.OPEN;
        this.yesCount = 0L;
        this.noCount = 0L;
    }

    public void incrementYes() {
        this.yesCount += 1;
    }

    public void incrementNo() {
        this.noCount += 1;
    }

    public void closeVote() {
        this.feedStatus = FeedStatus.CLOSED;
    }

    public void closeReport() {
        this.reportStatus = ReportStatus.NONE;
    }

    public void deleteByReport() {
        this.reportStatus = ReportStatus.DELETED;
    }

    public boolean isVoteOpen() {
        return this.feedStatus == FeedStatus.OPEN;
    }

    public boolean isExpired() {
        return this.getCreatedAt() != null
                && LocalDateTime.now().isAfter(this.getCreatedAt().plusHours(VOTE_DEADLINE_HOURS));
    }

    public void report() {
        this.reportStatus = ReportStatus.REPORTED;
    }

    public boolean isReported() {
        return this.reportStatus == ReportStatus.REPORTED || this.reportStatus == ReportStatus.DELETED;
    }

    public boolean isOwner(User user) {
        return this.user.getId().equals(user.getId());
    }
}
