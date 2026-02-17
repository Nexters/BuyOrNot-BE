package com.nexters.sseotdabwa.domain.votes.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import com.nexters.sseotdabwa.domain.votes.enums.VoteType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vote_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, length = 10)
    private VoteChoice choice;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 10)
    private VoteType voteType;

    @Builder
    public VoteLog(User user, Feed feed, VoteChoice choice, VoteType voteType) {
        this.user = user;
        this.feed = feed;
        this.choice = choice;
        this.voteType = voteType;
    }
}
