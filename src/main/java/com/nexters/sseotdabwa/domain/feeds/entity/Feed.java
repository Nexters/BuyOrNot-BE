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

@Entity
@Table(name = "feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column
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

    @Builder
    public Feed(User user, String content, Long price, FeedCategory category) {
        this.user = user;
        this.content = content;
        this.price = price;
        this.category = category;
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
}
