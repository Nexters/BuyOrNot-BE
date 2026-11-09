package com.nexters.sseotdabwa.domain.comments.entity;

import com.nexters.sseotdabwa.common.entity.BaseEntity;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 공감. 원댓글/답글 모두 가능하다.
 * - 회원 공감: user가 있고 (comment_id, user_id) 유니크로 중복 공감을 막는다 (취소 가능)
 * - 비회원 공감: user가 null. 누가 눌렀는지 구분하지 않고 누를 때마다 1건씩 쌓인다 (취소 없음)
 *   (MySQL 유니크는 NULL끼리 비교하지 않아 비회원 행은 여러 건 저장된다)
 */
@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comment_likes_comment_user", columnNames = {"comment_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_comment_likes_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private CommentLike(Comment comment, User user) {
        this.comment = comment;
        this.user = user;
    }

    public static CommentLike ofMember(Comment comment, User user) {
        return new CommentLike(comment, user);
    }

    public static CommentLike ofGuest(Comment comment) {
        return new CommentLike(comment, null);
    }
}
