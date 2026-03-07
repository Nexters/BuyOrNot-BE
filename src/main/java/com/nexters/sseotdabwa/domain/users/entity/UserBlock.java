package com.nexters.sseotdabwa.domain.users.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단 관계 엔티티
 *
 * 정책:
 * - 단방향 차단 (A가 B 차단 -> A에게만 B가 숨김)
 * - (user_id, blocked_user_id) 유니크 보장
 */
@Entity
@Table(
        name = "user_blocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_blocks_user_blocked", columnNames = {"user_id", "blocked_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_blocks_user_id", columnList = "user_id"),
                @Index(name = "idx_user_blocks_blocked_user_id", columnList = "blocked_user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 차단한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 차단된 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    @Builder
    private UserBlock(User user, User blockedUser) {
        this.user = user;
        this.blockedUser = blockedUser;
    }
}
