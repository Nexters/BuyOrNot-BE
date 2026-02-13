package com.nexters.sseotdabwa.domain.votes.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.feeds.entity.Feed;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.votes.enums.VoteChoice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "vote_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vote_log_user_feed",
                        columnNames = {"user_id", "feed_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, length = 10)
    private VoteChoice choice;

    @Builder
    public VoteLog(User user, Feed feed, VoteChoice choice) {
        this.user = user;
        this.feed = feed;
        this.choice = choice;
    }
}
